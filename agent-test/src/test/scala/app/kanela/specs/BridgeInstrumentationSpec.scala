/*
 * =========================================================================================
 * Copyright © 2013-2019 the kamon project <http://kamon.io/>
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

package app.kanela.specs

import app.kanela.cases.bridge.PrivateClass
import app.kanela.instrumentation.PrivateClassBridge
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}


class BridgeInstrumentationSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  "The Bridge" should
    "generate an accessor method for a private field or method" in {
        val privateClass = new PrivateClass()
        privateClass.asInstanceOf[PrivateClassBridge].getSecureWord shouldBe "Hello!!"
        privateClass.asInstanceOf[PrivateClassBridge].getSecret shouldBe "SuperSecret"
  }
}
