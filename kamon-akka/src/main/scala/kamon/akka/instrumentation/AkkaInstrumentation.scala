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

package kamon.servlet.instrumentation

import javax.servlet.{ ServletRequest, ServletResponse }

import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers._
import kamon.agent.scala.KamonInstrumentation
import kamon.servlet.instrumentation.advisor.{ ResponseStatusAdvisor, ServiceMethodAdvisor }
import kamon.servlet.instrumentation.mixin.HttpServletResponseMixin

class ServletInstrumentation extends KamonInstrumentation {

  /**
   * javax.servlet.Servlet::service
   */
  val ServiceMethod: Junction[MethodDescription] = named("service")
    .and(takesArguments[MethodDescription](classOf[ServletRequest], classOf[ServletResponse]))
    .and(not(isAbstract()))

  forSubtypeOf("javax.servlet.Servlet") { builder ⇒
    builder
      .withAdvisorFor(ServiceMethod, classOf[ServiceMethodAdvisor])
      .build()
  }

  /**
   * javax.servlet.http.HttpServletResponse::setStatus
   * javax.servlet.http.HttpServletResponse::sendError
   */
  val SetStatusMethod: Junction[MethodDescription] = named("setStatus")
  val SendErrorMethod: Junction[MethodDescription] = named("sendError").and(takesArguments(classOf[Int]))

  forSubtypeOf("javax.servlet.http.HttpServletResponse") { builder ⇒
    builder
      .withMixin(classOf[HttpServletResponseMixin])
      .withAdvisorFor(SetStatusMethod, classOf[ResponseStatusAdvisor])
      .withAdvisorFor(SendErrorMethod, classOf[ResponseStatusAdvisor])
      .build()
  }
}
