package akka.kamon.instrumentation

import akka.actor.Cell
import kamon.Kamon
import kamon.akka.RouterMetrics
import kamon.metric.Entity
import kamon.util.RelativeNanoTimestamp

trait RouterMonitor {
  def processMessageStart(): RelativeNanoTimestamp
  def processMessageEnd(timestampBeforeProcessing:RelativeNanoTimestamp): Unit
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

  def processMessageEnd(timestampBeforeProcessing:RelativeNanoTimestamp): Unit = {
      val timestampAfterProcessing = RelativeNanoTimestamp.now
      val routingTime = timestampAfterProcessing - timestampBeforeProcessing

      routerMetrics.routingTime.record(routingTime.nanos)
  }

  def processFailure(failure: Throwable): Unit = {}
  def routeeAdded(): Unit = {}
  def routeeRemoved(): Unit = {}
  def cleanup(): Unit = Kamon.metrics.removeEntity(entity)
}