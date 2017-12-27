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

import java.util.concurrent.CopyOnWriteArrayList
import java.util.{List => JList}

import kamon.agent.api.instrumentation.mixin.Initializer

import scala.collection.concurrent.TrieMap

class MonitorMixin extends MonitorAware {

  private var _execTimings: TrieMap[String, CopyOnWriteArrayList[Long]] = _

  def execTimings: TrieMap[String, CopyOnWriteArrayList[Long]] = this._execTimings

  def execTimings(methodName: String): JList[Long] = this._execTimings.getOrElse(methodName, new CopyOnWriteArrayList())

  def addExecTimings(methodName: String, time: Long): JList[Long] = {
    val update = this._execTimings.getOrElseUpdate(methodName, new CopyOnWriteArrayList())
    update.add(time)
    update
  }

  @Initializer
  def init(): Unit = this._execTimings = TrieMap[String, CopyOnWriteArrayList[Long]]()
}

trait MonitorAware {
  def execTimings(methodName: String): JList[Long]
  def execTimings: TrieMap[String, CopyOnWriteArrayList[Long]]
  def addExecTimings(methodName: String, time: Long):JList[Long]
}
