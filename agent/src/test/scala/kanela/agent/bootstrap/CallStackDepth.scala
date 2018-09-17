/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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

package kanela.agent.bootstrap

import kanela.agent.bootstrap.stack.CallStackDepth
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class CallStackDepth extends Matchers with WordSpecLike with BeforeAndAfterAll {
  "The CallStackDepth" should {
    "increment the value in successive calls and then reset" in {
      val a = new Object
      val b = new Object

      CallStackDepth.incrementFor(a) shouldBe 0
      CallStackDepth.incrementFor(b) shouldBe 0

      CallStackDepth.incrementFor(a) shouldBe 1
      CallStackDepth.incrementFor(b) shouldBe 1

      CallStackDepth.resetFor(a)

      CallStackDepth.incrementFor(a) shouldBe 0
      CallStackDepth.incrementFor(b) shouldBe 2

      CallStackDepth.resetFor(b)

      CallStackDepth.incrementFor(a) shouldBe 1
      CallStackDepth.incrementFor(b) shouldBe 0

      CallStackDepth.resetFor(a)
      CallStackDepth.resetFor(b)

      CallStackDepth.incrementFor(a) shouldBe 0
      CallStackDepth.incrementFor(b) shouldBe 0

      CallStackDepth.incrementFor(a) shouldBe 1
      CallStackDepth.incrementFor(b) shouldBe 1
    }
  }
}

