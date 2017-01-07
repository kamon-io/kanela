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

package app.kamon.instrumentation.advisor

import kamon.agent.libs.net.bytebuddy.asm.Advice._
import collection.mutable.ListBuffer

class TestMethodAdvisor
object TestMethodAdvisor {

  @OnMethodEnter
  def onMethodEnter(@Argument(value = 0, readOnly = false) values: ListBuffer[String]): Unit = {
    values += "enter"
  }

  @OnMethodExit
  def onMethodExit(@Return(readOnly = false) values: ListBuffer[String]): Unit = {
    values += "exit"
  }

}