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

package app.kanela.specs

import app.kanela.cases.simple.TestClass
import kanela.agent.attacher.Attacher
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner

import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class AttachInRuntimeSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  "Kanela agent" should "be able to attach in runtime and instrument the loaded classes" in {
    val testClass = new TestClass()
    testClass.addValue(ListBuffer()) shouldBe ListBuffer("body")

    Attacher.attach()

    testClass.addValue(ListBuffer()) shouldBe ListBuffer("enter", "body", "exit")
  }
}
