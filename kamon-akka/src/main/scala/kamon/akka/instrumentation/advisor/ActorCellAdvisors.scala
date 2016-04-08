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

package akka.kamon.instrumentation.advisor

import akka.actor.{ActorRef, ActorSystem, Cell, InternalActorRef}
import akka.dispatch.Envelope
import akka.kamon.instrumentation.{ActorMonitor, RouterMonitor}
import akka.routing.RoutedActorCell
import kamon.agent.libs.net.bytebuddy.asm.Advice._
import kamon.akka.instrumentation.mixin.{ActorInstrumentationAware, InstrumentedEnvelope, RouterInstrumentationAware}
import kamon.util.RelativeNanoTimestamp

import scala.collection.immutable

trait ActorInstrumentationSupport {
  def actorInstrumentation(cell: Cell): ActorMonitor = cell.asInstanceOf[ActorInstrumentationAware].actorInstrumentation
}

/**
 * Advisor for akka.actor.ActorCell::constructor
 */
class ActorCellConstructorAdvisor
object ActorCellConstructorAdvisor {
  @OnMethodExit(onThrowable = false)
  def onExit(@This cell: Cell,
    @Argument(0) system: ActorSystem,
    @Argument(1) ref: ActorRef,
    @Argument(4) parent: InternalActorRef): Unit = {

    cell.asInstanceOf[ActorInstrumentationAware].setActorInstrumentation(ActorMonitor.createActorMonitor(cell, system, ref, parent))
  }
}

/**
 * Advisor for akka.actor.ActorCell::invoke
 */
class InvokeMethodAdvisor
object InvokeMethodAdvisor extends ActorInstrumentationSupport {

  @OnMethodEnter
  def onEnter(@This cell: Cell,
    @Argument(0) envelope: Envelope): RelativeNanoTimestamp = {

    actorInstrumentation(cell).processMessageStart(envelope.asInstanceOf[InstrumentedEnvelope].envelopeContext())
  }

  @OnMethodExit
  def onExit(@This cell: Cell,
    @Enter timestampBeforeProcessing: RelativeNanoTimestamp,
    @Argument(0) envelope: Envelope): Unit = {

    actorInstrumentation(cell).processMessageEnd(timestampBeforeProcessing, envelope.asInstanceOf[InstrumentedEnvelope].envelopeContext())
  }
}

/**
 * Advisor for akka.actor.ActorCell::sendMessage
 * Advisor for akka.actor.UnstartedCell::sendMessage
 * Advisor for akka.dispatch.RoutedActorCell::sendMessage
 */
class SendMessageMethodAdvisor
object SendMessageMethodAdvisor extends ActorInstrumentationSupport {
  @OnMethodEnter
  def onEnter(@This cell: Cell,
    @Argument(0) envelope: Envelope): Unit = {
      envelope.asInstanceOf[InstrumentedEnvelope].setEnvelopeContext(actorInstrumentation(cell).captureEnvelopeContext())
  }
}

/**
 * Advisor for akka.actor.ActorCell::stop
 */
class StopMethodAdvisor
object StopMethodAdvisor extends ActorInstrumentationSupport {
  @OnMethodExit
  def onExit(@This cell: Cell): Unit = {
    actorInstrumentation(cell).cleanup()

    // The Stop can't be captured from the RoutedActorCell so we need to put this piece of cleanup here.
    if (cell.isInstanceOf[RoutedActorCell]) {
      cell.asInstanceOf[RouterInstrumentationAware].routerInstrumentation.cleanup()
    }
  }
}

/**
 * Advisor for akka.actor.ActorCell::handleInvokeFailure
 */
class HandleInvokeFailureMethodAdvisor
object HandleInvokeFailureMethodAdvisor extends ActorInstrumentationSupport {
  @OnMethodEnter
  def onEnter(@This cell: Cell,
    @Argument(0) childrenNotToSuspend: immutable.Iterable[ActorRef],
    @Argument(1) failure: Throwable): Unit = {

    actorInstrumentation(cell).cleanup()

    // The Stop can't be captured from the RoutedActorCell so we need to put this piece of cleanup here.
    if (cell.isInstanceOf[RoutedActorCell]) {
      cell.asInstanceOf[RouterInstrumentationAware].routerInstrumentation.cleanup()
    }
  }
}

/**
 * Advisor for akka.actor.UnstartedCell::constructor
 */
class RepointableActorCellConstructorAdvisor
object RepointableActorCellConstructorAdvisor {
  @OnMethodExit(onThrowable = false)
  def onExit(@This cell: Cell,
    @Argument(0) system: ActorSystem,
    @Argument(1) ref: ActorRef,
    @Argument(3) parent: InternalActorRef): Unit = {

    cell.asInstanceOf[ActorInstrumentationAware].setActorInstrumentation(ActorMonitor.createActorMonitor(cell, system, ref, parent))
  }
}

/**
 * Advisor for akka.dispatch.RoutedActorCell::constructor
 */
class RoutedActorCellConstructorAdvisor
object RoutedActorCellConstructorAdvisor {
  @OnMethodExit
  def onExit(@This cell: RoutedActorCell): Unit = {
    cell.asInstanceOf[RouterInstrumentationAware].setRouterInstrumentation(RouterMonitor.createRouterInstrumentation(cell))
  }
}

/**
 * Advisor for akka.dispatch.RoutedActorCell::constructor
 */
class SendMessageMethodAdvisorForRouter
object SendMessageMethodAdvisorForRouter extends ActorInstrumentationSupport {

  def routerInstrumentation(cell: Cell): RouterMonitor = cell.asInstanceOf[RouterInstrumentationAware].routerInstrumentation

  @OnMethodEnter
  def onEnter(@This cell: RoutedActorCell): RelativeNanoTimestamp = {
    routerInstrumentation(cell).processMessageStart()
  }

  @OnMethodExit
  def onExit(@This cell: RoutedActorCell,
    @Enter timestampBeforeProcessing: RelativeNanoTimestamp): Unit = {

    routerInstrumentation(cell).processMessageEnd(timestampBeforeProcessing)
  }
}

