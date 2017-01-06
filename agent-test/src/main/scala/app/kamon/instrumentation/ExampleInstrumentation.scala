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

import java.util.concurrent.Callable

import kamon.agent.api.instrumentation.Initializer
import kamon.agent.libs.net.bytebuddy.asm.Advice.{ Enter, OnMethodEnter, OnMethodExit }
import kamon.agent.libs.net.bytebuddy.implementation.bind.annotation.{ RuntimeType, SuperCall }
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers.named
import kamon.agent.scala

class ExampleInstrumentation extends scala.KamonInstrumentation {

  forTargetType("app.kamon.instrumentation.ExampleClass") { builder ⇒
    builder.withMixin(classOf[MixinTest])
      .withAdvisorFor(named("hello"), classOf[MethodAdvisor])
      .build()
  }

  forSubtypeOf("app.kamon.instrumentation.ExampleClass") { builder ⇒
    builder.withAdvisorFor(named("bye"), classOf[MethodAdvisor])
      .build()
  }
}

class MethodAdvisor
object MethodAdvisor {
  @OnMethodEnter
  def onMethodEnter(): Long = {
    System.currentTimeMillis() // Return current time, entering as parameter in the onMethodExist
  }

  @OnMethodExit
  def onMethodExit(@Enter start: Long): Unit = {
    println(s"Method took ${System.currentTimeMillis() - start} ms.")
  }
}

class MixinTest extends Serializable {
  var a: String = _

  @Initializer
  def init() = this.a = { println("HeeeeeLooooo"); "fruta" }

}

object ExampleClassInterceptor {
  @RuntimeType
  def prepareStatement(@SuperCall callable: Callable[_]): Any = {
    callable.call()
  }
}

final case class ExampleClass() {
  def hello() = println("Hi, all")
  def bye() = { println("good bye"); Thread.sleep(100) }
}