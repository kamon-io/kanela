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

import app.kamon.instrumentation.mixin.MonitorAware
import kamon.agent.libs.net.bytebuddy.asm.Advice._

class FakeWorkerAdvisor
object FakeWorkerAdvisor {

  @OnMethodEnter
  def onMethodEnter(): Long = {
    System.nanoTime() // Return current time, entering as parameter in the onMethodExist
  }

  @OnMethodExit
  def onMethodExit(@This instance: Object, @Enter start: Long, @Origin origin: String): Unit = {
    val timing = System.nanoTime() - start
    instance.asInstanceOf[MonitorAware].addExecTimings(origin, timing)
    println(s"Method $origin was executed in $timing ns.")
  }
}