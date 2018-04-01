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

import app.kanela.cases.simple.{SpyAware, TestClass}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

class SimpleInstrumentationSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  "An Advisor with OnMethodEnter and OnMethodExit" should "be able to instrument a specific method of a class" in {
    val testClass = new TestClass()
    testClass.addValue(ListBuffer()) shouldBe ListBuffer("enter", "body", "exit")
  }

  "A Mixin" should "introduce a Type to a simple class" in {
    new TestClass().asInstanceOf[SpyAware]
  }

  it should "be able to initialize any value from a @Initializer method" in {
    val testClass = new TestClass()
    testClass.addValue(ListBuffer())
    testClass.asInstanceOf[SpyAware].tracks shouldBe ListBuffer("init", "enter", "exit")
  }

}

