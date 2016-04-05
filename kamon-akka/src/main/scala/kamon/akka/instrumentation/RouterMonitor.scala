/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package akka.kamon.instrumentation

import akka.actor.Cell
import kamon.Kamon
import kamon.akka.RouterMetrics
import kamon.metric.Entity
import kamon.util.RelativeNanoTimestamp

trait RouterMonitor {
  def processMessageStart(): RelativeNanoTimestamp
  def processMessageEnd(timestampBeforeProcessing: RelativeNanoTimestamp): Unit
  def processFailure(failure: Throwable): Unit
  def cleanup(): Unit

  def routeeAdded(): Unit
  def routeeRemoved(): Unit
}

object RouterMonitor {

  def createRouterInstrumentation(cell: Cell): RouterMonitor = {
    val cellInfo = CellInfo.cellInfoFor(cell, cell.system, cell.self, cell.parent)
    def routerMetrics = Kamon.metrics.entity(RouterMetrics, cellInfo.entity)

    if (cellInfo.isTracked)
      new MetricsOnlyRouterMonitor(cellInfo.entity, routerMetrics)
    else NoOpRouterMonitor
  }
}

object NoOpRouterMonitor extends RouterMonitor {
  def processMessageStart(): RelativeNanoTimestamp = RelativeNanoTimestamp.zero
  def processMessageEnd(timestampBeforeProcessing: RelativeNanoTimestamp): Unit = {}
  def processFailure(failure: Throwable): Unit = {}
  def routeeAdded(): Unit = {}
  def routeeRemoved(): Unit = {}
  def cleanup(): Unit = {}
}

class MetricsOnlyRouterMonitor(entity: Entity, routerMetrics: RouterMetrics) extends RouterMonitor {

  def processMessageStart(): RelativeNanoTimestamp = RelativeNanoTimestamp.now

  def processMessageEnd(timestampBeforeProcessing: RelativeNanoTimestamp): Unit = {
    val timestampAfterProcessing = RelativeNanoTimestamp.now
    val routingTime = timestampAfterProcessing - timestampBeforeProcessing

    routerMetrics.routingTime.record(routingTime.nanos)
  }

  def processFailure(failure: Throwable): Unit = {}
  def routeeAdded(): Unit = {}
  def routeeRemoved(): Unit = {}
  def cleanup(): Unit = Kamon.metrics.removeEntity(entity)
}