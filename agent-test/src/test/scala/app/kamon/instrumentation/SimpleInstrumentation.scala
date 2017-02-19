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

package app.kamon.instrumentation

import app.kamon.instrumentation.advisor.{ SpyAdvisor, TestMethodAdvisor }
import app.kamon.instrumentation.mixin.SpyMixin
import kamon.agent.scala.KamonInstrumentation

class SimpleInstrumentation extends KamonInstrumentation {
  val methodName = named("addValue")

  forTargetType("app.kamon.cases.simple.TestClass") { builder ⇒
    builder
      .withMixin(classOf[SpyMixin])
      .withAdvisorFor(methodName, classOf[TestMethodAdvisor])
      .withAdvisorFor(methodName, classOf[SpyAdvisor])
      .build()
  }
}

