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

class MixinSpec extends munit.FunSuite {

  test("should add a mixin to a class") {
    val targetInstance = new TargetClassForMixin()

    assert(targetInstance.isInstanceOf[GetAnotherNumber])
    assertEquals(targetInstance.giveMeANumber(), 32)
    assertEquals(targetInstance.asInstanceOf[GetAnotherNumber].getAnotherNumber(), 42)
  }

}

class TargetClassForMixin {
  def giveMeANumber(): Int = 32
}

trait GetAnotherNumber {
  def getAnotherNumber(): Int
}

object GetAnotherNumber {
  class Mixin extends GetAnotherNumber {
    def getAnotherNumber(): Int = 42
  }
}

class MixinSpecInstrumentation extends InstrumentationBuilder {

  onType("kanela.agent.TargetClassForMixin")
    .mixin(classOf[GetAnotherNumber.Mixin])
}
