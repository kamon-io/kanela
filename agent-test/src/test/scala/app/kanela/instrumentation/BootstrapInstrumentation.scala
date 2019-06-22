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

package app.kanela.instrumentation

import java.util.concurrent.Callable

import app.kanela.cases.replacer.app.kanela.instrumentation.MySuperInsterceptor
import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.libs.net.bytebuddy.asm.Advice.OnMethodEnter
import kanela.agent.libs.net.bytebuddy.implementation.bind.annotation.SuperCall

class BootstrapInstrumentation extends InstrumentationBuilder {
  onType( "java.net.HttpURLConnection")
//    .intercept(method("getRequestMethod"), classOf[MySuperInsterceptor])
    .intercept(method("getRequestMethod"), classOf[MyInterceptor])
}


class MyInterceptor
object MyInterceptor {
//  @throws[Exception]
//  @OnMethodEnter
//  def onEnter() = {
  def intercept(@SuperCall zuper: Callable[String]): String = {
    System.out.println("Intercepted!")
    zuper.call
  }
}
