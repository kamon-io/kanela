/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

package app.kamon.instrumentation.mixin

import java.util.concurrent.{ ConcurrentHashMap, ConcurrentMap }
import kamon.agent.api.instrumentation.Initializer

class MonitorMixin extends MonitorAware {
  import collection.JavaConverters._

  private var _execTimings: ConcurrentMap[String, Vector[Long]] = _

  def execTimings: Map[String, Vector[Long]] = this._execTimings.asScala.toMap

  def execTimings(methodName: String): Vector[Long] = this._execTimings.getOrDefault(methodName, Vector.empty)

  def addExecTimings(methodName: String, time: Long): Vector[Long] = {
    this._execTimings.compute(methodName, (_, oldValues) ⇒ Option(oldValues).map(_ :+ time).getOrElse(Vector(time)))
  }

  @Initializer
  def init(): Unit = this._execTimings = new ConcurrentHashMap()

}

trait MonitorAware {
  def execTimings(methodName: String): Vector[Long]
  def execTimings: Map[String, Vector[Long]]
  def addExecTimings(methodName: String, time: Long): Vector[Long]
}
