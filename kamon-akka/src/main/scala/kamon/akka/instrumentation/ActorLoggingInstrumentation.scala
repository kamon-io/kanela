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

import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.implementation.bind.annotation.{ RuntimeType, Super }
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers._
import kamon.agent.scala.KamonInstrumentation
import kamon.akka.instrumentation.mixin.TraceContextMixin

class ActorLoggingInstrumentation extends KamonInstrumentation {

  /**
   * Instrument:
   *
   *  akka.dispatch.Dispatchers::lookup
   *
   */
  forSubtypeOf("akka.event.Logging.LogEvent") { builder ⇒
    builder
      .withMixin(classOf[TraceContextMixin])
      .build()
  }

//  val WithMdcMethod: Junction[MethodDescription] = named("withMdc")
//
//  forSubtypeOf("akka.event.slf4j.Slf4jLogger") { builder ⇒
//    builder
//      .withTransformationFor(WithMdcMethod, classOf[WithMdcMethodTransformer])
//      .build()
//  }
}

class WithMdcMethodTransformer
object WithMdcMethodTransformer {
  @RuntimeType
  def withMdcInvocation(@Super runnable: Runnable): Unit = {
    //    Tracer.withContext(logEvent.asInstanceOf[TraceContextAware].traceContext) {
    //      MdcKeysSupport.withMdc(runnable.run())
    //}
  }
}
