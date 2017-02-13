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

package kamon.play.instrumentation

import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers.named
import kamon.agent.scala.KamonInstrumentation
import kamon.play.instrumentation.interceptor.LogInterceptor

class LoggerLikeInstrumentation extends KamonInstrumentation {

  val LogMethod: Junction[MethodDescription] = {
    named("info")
      .or(named("debug")
        .or(named("warn")
          .or(named("error")
            .or(named("trace")))))
  }

  forSubtypeOf("play.api.LoggerLike") { builder =>
    builder
      .withTransformationFor(LogMethod, classOf[LogInterceptor])
      .build()
  }

  forSubtypeOf("play.LoggerLike") { builder =>
    builder
      .withTransformationFor(LogMethod, classOf[LogInterceptor])
      .build()
  }
}
