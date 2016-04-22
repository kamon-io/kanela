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

import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers._
import kamon.agent.scala.KamonInstrumentation
import kamon.servlet.instrumentation.advisor._
import kamon.servlet.instrumentation.mixin.AsyncContextMixin
import kamon.trace.{ TraceContextAware, Tracer }

class AsyncServletInstrumentation extends KamonInstrumentation {

  /**
   * javax.servlet.AsyncContext::start
   * javax.servlet.AsyncContext::complete
   */
  val StartMethod: Junction[MethodDescription] = named("start")
  val CompleteMethod: Junction[MethodDescription] = named("complete")

  forSubtypeOf("javax.servlet.AsyncContext") { builder ⇒
    builder
      .withMixin(classOf[AsyncContextMixin])
      .withAdvisorFor(StartMethod, classOf[ParameterWrapperAdvisor])
      .withAdvisorFor(CompleteMethod, classOf[CompleteAdvisor])
      .build()
  }

  /**
   * javax.servlet.ServletRequest::startAsync()
   * javax.servlet.ServletRequest::startAsync(request,response)
   */
  val StartAsyncMethod: Junction[MethodDescription] = named("startAsync")

  forSubtypeOf("javax.servlet.ServletRequest") { builder ⇒
    builder
      .withAdvisorFor(StartAsyncMethod, classOf[StartAsyncAdvisor])
      .build()
  }
}

object AsyncServletInstrumentation {
  /**
   * Wrap a Runnable in order to propagate the current TraceContext
   */
  class TraceContextAwareRunnable(underlying: Runnable) extends TraceContextAware with Runnable {
    val traceContext = Tracer.currentContext

    override def run(): Unit = {
      Tracer.withContext(traceContext) {
        underlying.run()
      }
    }
  }
}