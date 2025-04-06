/*
 *  ==========================================================================================
 *  Copyright © 2013-2025 The Kamon Project <https://kamon.io/>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 *  ==========================================================================================
 */

package kanela.agent

import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.libs.net.bytebuddy.asm.Advice

class InterceptorSpec extends munit.FunSuite {

  test("should delegate method calls to a target object") {
    val targetNumber = new InterceptorSpec.TargetClass().giveMeANumber()
    assertEquals(targetNumber, 42)
  }

  test("should delegate method calls to a target class") {
    val targetNumber = new InterceptorSpec.TargetClass().giveMeAnotherNumber()
    assertEquals(targetNumber, 52)
  }

}

object InterceptorSpec {
  class TargetClass {
    def giveMeANumber(): Int = 32
    def giveMeAnotherNumber(): Int = 32
  }
}

class InterceptorViaInstance {
  def giveMeANumber(): Int = 42
}

class InterceptorViaClass
object InterceptorViaClass {
  def giveMeAnotherNumber(): Int = 52
}

class InterceptorSpecInstrumentation extends InstrumentationBuilder {

  onType("kanela.agent.InterceptorSpec$TargetClass")
    .intercept(method("giveMeANumber"), new InterceptorViaInstance())
    .intercept(method("giveMeAnotherNumber"), classOf[InterceptorViaClass])
}
