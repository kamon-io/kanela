/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

package app.kamon.instrumentation

import app.kamon.instrumentation.advisor.{ SpyAdvisor, TestMethodAdvisor }
import app.kamon.instrumentation.mixin.SpyMixin
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers.named
import kamon.agent.scala

class SimpleInstrumentation extends scala.KamonInstrumentation {
  val methodName: Junction[MethodDescription] = named("addValue")

  forTargetType("app.kamon.specs.TestClass") { builder ⇒
    builder
      .withMixin(classOf[SpyMixin])
      .withAdvisorFor(methodName, classOf[TestMethodAdvisor])
      .withAdvisorFor(methodName, classOf[SpyAdvisor])
      .build()
  }
}

