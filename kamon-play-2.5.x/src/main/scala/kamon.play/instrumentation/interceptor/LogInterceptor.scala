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

import kamon.agent.libs.net.bytebuddy.asm.Advice
import kamon.agent.libs.net.bytebuddy.implementation.bind.annotation.{RuntimeType, SuperCall}
import kamon.trace.Tracer
import kamon.trace.logging.MdcKeysSupport
import org.slf4j.MDC

/**
  * Interceptor for play.api.LoggerLike::{ info | debug | warn | error | trace }
  * Interceptor for play.LoggerLike::{ info | debug | warn | error | trace }
  */
class LogInterceptor
object LogInterceptor {
  @RuntimeType
  def aroundLog(@SuperCall callable: Callable[Any]): Any = MdcKeysSupport.withMdc {
    callable.call()
  }
}

class LogAdvisor
object LogAdvisor {
  @Advice.OnMethodEnter
  def onEnter(@Advice.Argument(0) argument: () => String): Iterable[String] =  MdcKeysSupport.copyToMdc(Tracer.currentContext)

  @Advice.OnMethodExit
  def onExit(@Advice.Enter keys:Iterable[String]): Unit = keys.foreach(key ⇒ MDC.remove(key))
}