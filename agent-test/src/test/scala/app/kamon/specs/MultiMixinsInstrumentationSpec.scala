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

package app.kamon.specs

import app.kamon.utils.AdditionalJVMParameters
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

@AdditionalJVMParameters(parameters = "-Dkamon.agent.instrumentations.0=app.kamon.instrumentation.MultiMixinsInstrumentation")
class MultiMixinsInstrumentationSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  "Multiple Mixins over a single subType" should "introduce all types appropriately" in {
    val mixinsClass = new WithMultiMixinsClass()
    mixinsClass.process shouldBe "Hi"
    mixinsClass.isInstanceOf[MixinAware1] shouldBe true
    mixinsClass.isInstanceOf[MixinAware2] shouldBe true
    mixinsClass.isInstanceOf[MixinAware3] shouldBe true
  }
}
class WithMultiMixinsClass {
  def process: String = "Hi"
}

trait MixinAware1 {
  def dummyMethod1: String
}

trait MixinAware2 {
  def dummyMethod2: String
}

trait MixinAware3 {
  def dummyMethod3: String
}
