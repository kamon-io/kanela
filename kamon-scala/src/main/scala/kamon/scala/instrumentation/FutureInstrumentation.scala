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

package kamon.scala.instrumentation

import kamon.agent.api.instrumentation.Initializer
import kamon.agent.libs.net.bytebuddy.asm.Advice.{ OnMethodEnter, OnMethodExit, This }
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers._
import kamon.agent.scala.KamonInstrumentation
import kamon.trace.{ TraceContext, TraceContextAware, Tracer }

class FutureInstrumentation extends KamonInstrumentation {

  /**
   * Instrument:
   *
   * scala.concurrent.impl.CallbackRunnable::run
   * scala.concurrent.impl.Future$PromiseCompletingRunnable::run
   *
   * Mix:
   *
   * scala.concurrent.impl.CallbackRunnable with kamon.trace.TraceContextAware
   * scala.concurrent.impl.Future$PromiseCompletingRunnable with kamon.trace.TraceContextAware
   *
   */

  val RunMethod: Junction[MethodDescription] = named("run")

  forTargetType("scala.concurrent.impl.CallbackRunnable") { builder ⇒
    builder
      .withMixin(classOf[TraceContextMixin])
      .withAdvisorFor(RunMethod, classOf[RunMethodAdvisor])
      .build()
  }

  forTargetType("scala.concurrent.impl.Future$PromiseCompletingRunnable") { builder ⇒
    builder
      .withMixin(classOf[TraceContextMixin])
      .withAdvisorFor(RunMethod, classOf[RunMethodAdvisor])
      .build()
  }
}

class TraceContextMixin extends TraceContextAware {
  var traceContext: TraceContext = _

  @Initializer
  def init(): Unit = this.traceContext = Tracer.currentContext
}

/**
 * Advisor for scala.concurrent.impl.CallbackRunnable::run
 * Advisor for scala.concurrent.impl.Future$PromiseCompletingRunnable::run
 */
class RunMethodAdvisor
object RunMethodAdvisor {
  @OnMethodEnter
  def onEnter(@This runnable: TraceContextAware): Unit = Tracer.setCurrentContext(runnable.traceContext)

  @OnMethodExit
  def onExit(): Unit = Tracer.currentContext.finish()
}
