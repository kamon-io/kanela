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

package kamon.play.instrumentation.interceptor

import java.util.concurrent.Callable

import kamon.agent.libs.net.bytebuddy.implementation.bind.annotation.{RuntimeType, SuperCall}
import kamon.play.PlayExtension
import kamon.trace.TraceContextAware
import play.api.mvc.RequestHeader
import play.api.mvc.Results.InternalServerError

/**
  * Interceptor for play.api.http.HttpErrorHandler::onClientError
  * Interceptor for play.api.http.HttpErrorHandler::onServerError
  */
class ErrorInterceptor
object ErrorInterceptor {

  @RuntimeType
  def onServerError(requestHeader: RequestHeader, ex: Throwable, @SuperCall r: Callable[Any]): Any = {
    requestHeader.asInstanceOf[TraceContextAware].traceContext.collect { ctx ⇒
      PlayExtension.httpServerMetrics.recordResponse(ctx.name, InternalServerError.header.status.toString)
    }
    r.call()
  }

  @RuntimeType
  def onClientError(requestHeader: RequestHeader, statusCode: Int,  message: String, @SuperCall r: Callable[Any]): Any= {
    requestHeader.asInstanceOf[TraceContextAware].traceContext.collect { ctx ⇒
      PlayExtension.httpServerMetrics.recordResponse(ctx.name, statusCode.toString)
    }
    r.call()
  }
}
