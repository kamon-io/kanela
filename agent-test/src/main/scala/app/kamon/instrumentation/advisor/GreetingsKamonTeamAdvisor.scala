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

import scala.util.Random

class GreetingsKamonTeamAdvisor
object GreetingsKamonTeamAdvisor {

  private val ratioInform = 1
  private val r = Random.self

  @OnMethodEnter
  def onMethodEnter(): Long = {
    System.nanoTime() // Return current time, entering as parameter in the onMethodExist
  }

  @OnMethodExit
  def onMethodExit(@This instance: Object, @Enter start: Long, @Origin origin: String): Unit = {
    val timing = System.nanoTime() - start
    val samples = instance.asInstanceOf[MonitorAware].addExecTimings(origin, timing)
    val average = samples.sum / samples.size
    val per95 = percentile(samples, 95)
    val per90 = percentile(samples, 90)
    val per80 = percentile(samples, 80)
    if (r.nextDouble() <= ratioInform)
      println(s"Method $origin was executed in $timing ns. # Samples: ${samples.size}. Avg: $average ns. P95: $per95 ns. P90: $per90 ns. P80: $per80 ns.")
  }

  def percentile(values: Vector[Long], quantile: Double): Double = {
    assert((quantile > 0) && (quantile <= 100), s"out of bounds quantile value: $quantile, must be in (0, 100]")
    values.length match {
      case 0 ⇒ 0
      case 1 ⇒ values.head // always return single value for n = 1
      case _ ⇒
        val length = values.length
        val pos = quantile * (length + 1) / 100
        val fpos = math.floor(pos)
        val intPos = fpos.toInt
        val dif = pos - fpos
        val sorted = values.sorted
        if (pos < 1) sorted.head
        else if (pos >= length) sorted.last
        else {
          val lower = sorted(intPos - 1)
          val upper = sorted(intPos)
          lower + dif * (upper - lower)
        }
    }
  }
}