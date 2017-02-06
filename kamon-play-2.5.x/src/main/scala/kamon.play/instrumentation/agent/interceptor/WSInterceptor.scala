/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.play.instrumentation.agent.interceptor

import java.util.concurrent.Callable

import kamon.agent.libs.net.bytebuddy.implementation.bind.annotation.{RuntimeType, SuperCall, This}
import kamon.play.PlayExtension
import kamon.trace.{SegmentCategory, Tracer}
import kamon.util.SameThreadExecutionContext
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class WSInterceptor
object WSInterceptor {

  @RuntimeType
  def aroundExecuteRequest(@SuperCall callable: Callable[Future[WSResponse]], @This request: WSRequest): Future[WSResponse] = {
    Tracer.currentContext.collect { ctx ⇒
      val segmentName = PlayExtension.generateHttpClientSegmentName(request)
      val segment = ctx.startSegment(segmentName, SegmentCategory.HttpClient, PlayExtension.SegmentLibraryName)
      val response = callable.call()

      response.onComplete {
        case Success(result) ⇒
          PlayExtension.httpClientMetrics.recordResponse(segmentName, result.status.toString)
          segment.finish()
        case Failure(error) ⇒
          segment.finishWithError(error)
      }(SameThreadExecutionContext)
      response
    } getOrElse callable.call()
  }

}
