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

package kamon.play.instrumentation.advisor

import kamon.Kamon.tracer
import kamon.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodEnter}
import kamon.play.PlayExtension
import kamon.trace.Tracer
import play.api.mvc.RequestHeader

/**
  * Advisor for play.api.http.DefaultHttpRequestHandler::routeRequest
  */
class RouteRequestAdvisor
object RouteRequestAdvisor {

  @OnMethodEnter
  def onEnter(@Argument(0) requestHeader: RequestHeader): Unit = {
    val token = if (PlayExtension.includeTraceToken) {
      requestHeader.headers.get(PlayExtension.traceTokenHeaderName)
    } else None

    Tracer.setCurrentContext(tracer.newContext("UnnamedTrace", token))
  }

}
