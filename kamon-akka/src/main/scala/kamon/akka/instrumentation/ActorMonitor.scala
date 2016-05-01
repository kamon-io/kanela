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

import akka.actor.{ ActorRef, ActorSystem, Cell }
import akka.kamon.instrumentation.ActorMonitors.{ TrackedActor, TrackedRoutee }
import kamon.Kamon
import kamon.akka.instrumentation.mixin.EnvelopeContext
import kamon.akka.{ ActorMetrics, RouterMetrics }
import kamon.metric.Entity
import kamon.trace.Tracer
import kamon.util.RelativeNanoTimestamp

trait ActorMonitor {
  def captureEnvelopeContext(): EnvelopeContext
  def processMessageStart(envelopeContext: EnvelopeContext): RelativeNanoTimestamp
  def processMessageEnd(timestampBeforeProcessing: RelativeNanoTimestamp, envelopeContext: EnvelopeContext): Unit
  def processFailure(failure: Throwable): Unit
  def cleanup(): Unit
}

object ActorMonitor {

  def createActorMonitor(cell: Cell, system: ActorSystem, ref: ActorRef, parent: ActorRef): ActorMonitor = {
    val cellInfo = CellInfo.cellInfoFor(cell, system, ref, parent)

    if (cellInfo.isRouter)
      ActorMonitors.ContextPropagationOnly
    else {
      if (cellInfo.isRoutee)
        createRouteeMonitor(cellInfo)
      else
        createRegularActorMonitor(cellInfo)
    }
  }

  def createRegularActorMonitor(cellInfo: CellInfo): ActorMonitor = {
    def actorMetrics = Kamon.metrics.entity(ActorMetrics, cellInfo.entity)

    if (cellInfo.isTracked)
      new TrackedActor(cellInfo.entity, actorMetrics)
    else ActorMonitors.ContextPropagationOnly
  }

  def createRouteeMonitor(cellInfo: CellInfo): ActorMonitor = {
    def routerMetrics = Kamon.metrics.entity(RouterMetrics, cellInfo.entity)

    if (cellInfo.isTracked)
      new TrackedRoutee(cellInfo.entity, routerMetrics)
    else ActorMonitors.ContextPropagationOnly
  }
}

object ActorMonitors {

  val ContextPropagationOnly = new ActorMonitor {
    def captureEnvelopeContext(): EnvelopeContext =
      EnvelopeContext(RelativeNanoTimestamp.now, Tracer.currentContext)

    def processMessageStart(envelopeContext: EnvelopeContext): RelativeNanoTimestamp = {
      Tracer.setCurrentContext(envelopeContext.context)
      RelativeNanoTimestamp.zero
    }

    def processMessageEnd(timestampBeforeProcessing: RelativeNanoTimestamp, envelopeContext: EnvelopeContext): Unit = {
      Tracer.currentContext.finish()
    }

    def processFailure(failure: Throwable): Unit = {}
    def cleanup(): Unit = {}

  }

  class TrackedActor(val entity: Entity, actorMetrics: ActorMetrics) extends ActorMonitor {
    def captureEnvelopeContext(): EnvelopeContext = {
      actorMetrics.mailboxSize.increment()
      EnvelopeContext(RelativeNanoTimestamp.now, Tracer.currentContext)
    }

    def processMessageStart(envelopeContext: EnvelopeContext): RelativeNanoTimestamp = {
      val timestampBeforeProcessing = RelativeNanoTimestamp.now
      Tracer.setCurrentContext(envelopeContext.context)
      timestampBeforeProcessing
    }

    def processMessageEnd(timestampBeforeProcessing: RelativeNanoTimestamp, envelopeContext: EnvelopeContext): Unit = {
      try envelopeContext.context.finish() finally {
        val timestampAfterProcessing = RelativeNanoTimestamp.now
        val timeInMailbox = timestampBeforeProcessing - envelopeContext.nanoTime
        val processingTime = timestampAfterProcessing - timestampBeforeProcessing

        actorMetrics.processingTime.record(processingTime.nanos)
        actorMetrics.timeInMailbox.record(timeInMailbox.nanos)
        actorMetrics.mailboxSize.decrement()
      }
    }
    def processFailure(failure: Throwable): Unit = actorMetrics.errors.increment()
    def cleanup(): Unit = Kamon.metrics.removeEntity(entity)
  }

  class TrackedRoutee(val entity: Entity, routerMetrics: RouterMetrics) extends ActorMonitor {
    def captureEnvelopeContext(): EnvelopeContext = EnvelopeContext(RelativeNanoTimestamp.now, Tracer.currentContext)

    def processMessageStart(envelopeContext: EnvelopeContext): RelativeNanoTimestamp = {
      val timestampBeforeProcessing = RelativeNanoTimestamp.now
      Tracer.setCurrentContext(envelopeContext.context)
      timestampBeforeProcessing
    }

    override def processMessageEnd(timestampBeforeProcessing: RelativeNanoTimestamp, envelopeContext: EnvelopeContext): Unit = {
      try envelopeContext.context.finish() finally {
        val timestampAfterProcessing = RelativeNanoTimestamp.now
        val timeInMailbox = timestampBeforeProcessing - envelopeContext.nanoTime
        val processingTime = timestampAfterProcessing - timestampBeforeProcessing

        routerMetrics.processingTime.record(processingTime.nanos)
        routerMetrics.timeInMailbox.record(timeInMailbox.nanos)
      }
    }

    def processFailure(failure: Throwable): Unit = routerMetrics.errors.increment()
    def cleanup(): Unit = {}
  }
}