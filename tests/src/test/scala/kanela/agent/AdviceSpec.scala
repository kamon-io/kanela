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

class AdviceSpec extends munit.FunSuite {

  test("should instrument a method enter with a plain class name advice") {
    new TestClass().hello()
    assertEquals(AdviceSpec.HelloCount, 2)
  }

  test("should instrument a method enter with a class reference") {
    new TestClass().hi()
    assertEquals(AdviceSpec.HiCount, 4)
  }

  test("should instrument a method enter with a Scala companion object marked with AdviceCompanion") {
    new TestClass().hola()
    assertEquals(AdviceSpec.HolaCount, 6)
  }

  test("should instrument a method exit with a plain class name advice") {
    new TestClass().goodbye()
    assertEquals(AdviceSpec.GoodbyeCount, 2)
  }

  test("should instrument a method exit with a class reference") {
    new TestClass().bye()
    assertEquals(AdviceSpec.ByeCount, 4)
  }

  test("should instrument a method exit with a Scala companion object marked with AdviceCompanion") {
    new TestClass().ciao()
    assertEquals(AdviceSpec.CiaoCount, 6)
  }

  test("should contain and log supressed exceptions thrown while running an OnMethodEnter advice") {
    new TestClass().faultyOnEnter()
    assertEquals(AdviceSpec.FaultyOnEnterCount, 8)
  }

  test("should contain and log supressed exceptions thrown while running an OnMethodExit advice") {
    new TestClass().faultyOnExit()
    assertEquals(AdviceSpec.FaultyOnExitCount, 8)
  }

  class TestClass {
    def hello(): Unit = {
      AdviceSpec.HelloCount += 1
    }

    def hi(): Unit = {
      AdviceSpec.HiCount += 2
    }

    def hola(): Unit = {
      AdviceSpec.HolaCount += 3
    }

    def goodbye(): Unit = {
      AdviceSpec.GoodbyeCount += 1
    }

    def bye(): Unit = {
      AdviceSpec.ByeCount += 2
    }

    def ciao(): Unit = {
      AdviceSpec.CiaoCount += 3
    }

    def faultyOnEnter(): Unit = {
      AdviceSpec.FaultyOnEnterCount += 4
    }

    def faultyOnExit(): Unit = {
      AdviceSpec.FaultyOnExitCount += 4
    }
  }
}

object AdviceSpec {
  var HelloCount = 0
  var HiCount = 0
  var HolaCount = 0
  var GoodbyeCount = 0
  var ByeCount = 0
  var CiaoCount = 0
  var FaultyOnEnterCount = 0
  var FaultyOnExitCount = 0
}

class AdviceSpecInstrumentation extends InstrumentationBuilder {
  onType("kanela.agent.AdviceSpec$TestClass")
    .advise(method("hello"), "kanela.agent.AddOneOnEnterAdvice")

  onType("kanela.agent.AdviceSpec$TestClass")
    .advise(method("hi"), classOf[AddTwoOnEnterAdvice])

  onType("kanela.agent.AdviceSpec$TestClass")
    .advise(method("hola"), classOf[AddThreeOnEnterAdvice])

  onType("kanela.agent.AdviceSpec$TestClass")
    .advise(method("goodbye"), "kanela.agent.AddOneOnExitAdvice")

  onType("kanela.agent.AdviceSpec$TestClass")
    .advise(method("bye"), classOf[AddTwoOnExitAdvice])

  onType("kanela.agent.AdviceSpec$TestClass")
    .advise(method("ciao"), classOf[AddThreeOnExitAdvice])

  onType("kanela.agent.AdviceSpec$TestClass")
    .advise(method("faultyOnEnter"), classOf[FaultyOnEnterAdvice])

  onType("kanela.agent.AdviceSpec$TestClass")
    .advise(method("faultyOnExit"), classOf[FaultyOnExitAdvice])
}

class AddOneOnEnterAdvice
object AddOneOnEnterAdvice {

  @Advice.OnMethodEnter
  def enter(): Unit = {
    AdviceSpec.HelloCount += 1
  }
}

class AddTwoOnEnterAdvice
object AddTwoOnEnterAdvice {

  @Advice.OnMethodEnter
  def enter(): Unit = {
    AdviceSpec.HiCount += 2
  }
}

class AddThreeOnEnterAdvice
object AddThreeOnEnterAdvice {

  @Advice.OnMethodEnter
  def enter(): Unit = {
    AdviceSpec.HolaCount += 3
  }
}

class AddOneOnExitAdvice
object AddOneOnExitAdvice {

  @Advice.OnMethodExit
  def exit(): Unit = {
    AdviceSpec.GoodbyeCount += 1
  }
}

class AddTwoOnExitAdvice
object AddTwoOnExitAdvice {

  @Advice.OnMethodExit
  def exit(): Unit = {
    AdviceSpec.ByeCount += 2
  }
}

class AddThreeOnExitAdvice
object AddThreeOnExitAdvice {

  @Advice.OnMethodExit
  def exit(): Unit = {
    AdviceSpec.CiaoCount += 3
  }
}

class FaultyOnEnterAdvice
object FaultyOnEnterAdvice {

  @Advice.OnMethodEnter(suppress = classOf[RuntimeException])
  def enter(): Unit = {
    AdviceSpec.FaultyOnEnterCount += 4
    sys.error("Failed after incrementing the FaultyOnEnterCount")
  }
}

class FaultyOnExitAdvice
object FaultyOnExitAdvice {

  @Advice.OnMethodExit(suppress = classOf[RuntimeException])
  def exit(): Unit = {
    AdviceSpec.FaultyOnExitCount += 4
    sys.error("Failed after incrementing the FaultyOnExitCount")
  }
}
