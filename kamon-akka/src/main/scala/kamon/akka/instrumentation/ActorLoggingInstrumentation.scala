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

package akka.kamon.instrumentation

import akka.event.Logging.LogEvent
import kamon.agent.libs.net.bytebuddy.asm.Advice
import kamon.agent.libs.net.bytebuddy.asm.Advice.{ Argument, Enter, OnMethodEnter, OnMethodExit }
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers._
import kamon.agent.scala.KamonInstrumentation
import kamon.akka.instrumentation.mixin.TraceContextMixin
import kamon.trace.logging.MdcKeysSupport
import kamon.trace.{ TraceContext, TraceContextAware, Tracer }
import org.slf4j.MDC

class ActorLoggingInstrumentation extends KamonInstrumentation {

  /**
   * Mix:
   *
   *  akka.event.Logging$LogEvent with kamon.trace.TraceContextAware
   *
   */
  forSubtypeOf("akka.event.Logging$LogEvent") { builder ⇒
    builder
      .withMixin(classOf[TraceContextMixin])
      .build()
  }

  /**
   * Instrument:
   *
   *  akka.event.slf4j.Slf4jLogger::withMdc
   *
   */
  val WithMdcMethod: Junction[MethodDescription] = named("withMdc")

  forTargetType("akka.event.slf4j.Slf4jLogger") { builder ⇒
    builder
      .withAdvisorFor(WithMdcMethod, classOf[WithMdcMethodAdvisor])
      .build()
  }
}

/**
 * Advisor for akka.event.slf4j.Slf4jLogger::withMdc
 */
class WithMdcMethodAdvisor
object WithMdcMethodAdvisor {
  @OnMethodEnter
  def onEnter(@Argument(1) logEvent: LogEvent): Iterable[String] = {
    val ctx: TraceContext = logEvent.asInstanceOf[TraceContextAware].traceContext
    Tracer.setCurrentContext(ctx)
    copyToMdc(ctx)
  }

  @OnMethodExit
  def onExit(@Enter keys: Iterable[String]): Unit = {
    keys.foreach(key ⇒ MDC.remove(key))
    Tracer.currentContext.finish()
  }
  //TODO:kamon.trace.logging.MdcKeysSupport#copyToMdc should be public :(
  private def copyToMdc(traceContext: TraceContext): Iterable[String] = Iterable.empty
}
