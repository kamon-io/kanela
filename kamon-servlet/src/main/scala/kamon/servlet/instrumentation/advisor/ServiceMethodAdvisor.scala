/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.servlet.instrumentation.advisor

import javax.servlet.http.HttpServletRequest
import javax.servlet.{ ServletRequest, ServletResponse }

import kamon.Kamon
import kamon.agent.libs.net.bytebuddy.asm.Advice.{ Argument, OnMethodEnter, OnMethodExit }
import kamon.servlet.ServletExtension
import kamon.servlet.instrumentation.mixin.TraceContextAwareExtension
import kamon.trace.Tracer

/**
 * Advisor for javax.servlet.Servlet::service
 */
class ServiceMethodAdvisor
object ServiceMethodAdvisor {
  @OnMethodEnter
  def onEnter(@Argument(0) request: ServletRequest, @Argument(1) response: ServletResponse): Unit = request match {
    case httpRequest: HttpServletRequest ⇒
      val traceName = ServletExtension.generateTraceName(httpRequest)
      val traceContext = Kamon.tracer.newContext(traceName)

      Tracer.setCurrentContext(traceContext)
      response.asInstanceOf[TraceContextAwareExtension].traceContext(traceContext)
    case other ⇒
  }

  @OnMethodExit
  def onExit(): Unit = Tracer.currentContext.finish()
}
