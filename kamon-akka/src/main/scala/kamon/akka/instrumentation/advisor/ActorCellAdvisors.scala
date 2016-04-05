package akka.kamon.instrumentation.advisor

import akka.actor.{ActorCell, ActorRef, ActorSystem, Cell, InternalActorRef}
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

class ActorCellConstructorAdvisor
object ActorCellConstructorAdvisor {
  @OnMethodExit
  def onExit(@This cell:Cell,
             @Argument(0) system: ActorSystem,
             @Argument(1) ref: ActorRef,
             @Argument(4) parent: InternalActorRef): Unit = {

    cell.asInstanceOf[ActorInstrumentationAware].setActorInstrumentation(ActorMonitor.createActorMonitor(cell, system, ref, parent))
  }
}

class InvokeMethodAdvisor
object InvokeMethodAdvisor extends ActorInstrumentationSupport{

  @OnMethodEnter
  def onEnter(@This cell: ActorCell,
              @Argument(0) envelope: Envelope): RelativeNanoTimestamp = {

    actorInstrumentation(cell).processMessageStart(envelope.asInstanceOf[InstrumentedEnvelope].envelopeContext())
  }

  @OnMethodExit
  def onExit(@This cell: ActorCell,
             @Enter timestampBeforeProcessing:RelativeNanoTimestamp,
             @Argument(0) envelope: Envelope): Unit = {

    actorInstrumentation(cell).processMessageEnd(timestampBeforeProcessing, envelope.asInstanceOf[InstrumentedEnvelope].envelopeContext())
  }
}

class SendMessageMethodAdvisor
object SendMessageMethodAdvisor extends ActorInstrumentationSupport {
  @OnMethodEnter
  def onEnter(@This cell: ActorCell,
              @Argument(0) envelope: Envelope): Unit = {

    envelope.asInstanceOf[InstrumentedEnvelope].setEnvelopeContext(actorInstrumentation(cell).captureEnvelopeContext())
  }
}

class StopMethodAdvisor
object StopMethodAdvisor extends ActorInstrumentationSupport {
  @OnMethodExit
  def onExit(@This cell: ActorCell): Unit = {
    actorInstrumentation(cell).cleanup()

    // The Stop can't be captured from the RoutedActorCell so we need to put this piece of cleanup here.
    if (cell.isInstanceOf[RoutedActorCell]) {
      cell.asInstanceOf[RouterInstrumentationAware].routerInstrumentation.cleanup()
    }
  }
}

class HandleInvokeFailureMethodAdvisor
object HandleInvokeFailureMethodAdvisor extends ActorInstrumentationSupport {
  @OnMethodEnter
  def onEnter(@This cell: ActorCell,
              @Argument(0) childrenNotToSuspend: immutable.Iterable[ActorRef],
              @Argument(0) failure: Throwable): Unit = {

    actorInstrumentation(cell).cleanup()

    // The Stop can't be captured from the RoutedActorCell so we need to put this piece of cleanup here.
    if (cell.isInstanceOf[RoutedActorCell]) {
      cell.asInstanceOf[RouterInstrumentationAware].routerInstrumentation.cleanup()
    }
  }
}


class RepointableActorCellConstructorAdvisor
object RepointableActorCellConstructorAdvisor {
  @OnMethodExit
  def onExit(@This cell:Cell,
             @Argument(0) system: ActorSystem,
             @Argument(1) ref: ActorRef,
             @Argument(3) parent: InternalActorRef): Unit = {

    cell.asInstanceOf[ActorInstrumentationAware].setActorInstrumentation(ActorMonitor.createActorMonitor(cell, system, ref, parent))
  }
}

class RoutedActorCellConstructorAdvisor
object RoutedActorCellConstructorAdvisor {
  @OnMethodExit
  def onExit(@This cell:RoutedActorCell): Unit = {
    cell.asInstanceOf[RouterInstrumentationAware].setRouterInstrumentation(RouterMonitor.createRouterInstrumentation(cell))
  }
}

class SendMessageMethodAdvisorForRouter
object SendMessageMethodAdvisorForRouter extends ActorInstrumentationSupport {

  def routerInstrumentation(cell: Cell): RouterMonitor = cell.asInstanceOf[RouterInstrumentationAware].routerInstrumentation

  @OnMethodEnter
  def onEnter(@This cell: RoutedActorCell): RelativeNanoTimestamp = {
    routerInstrumentation(cell).processMessageStart()
  }

  @OnMethodExit
  def onExit(@This cell: RoutedActorCell,
             @Enter timestampBeforeProcessing:RelativeNanoTimestamp): Unit = {

    routerInstrumentation(cell).processMessageEnd(timestampBeforeProcessing)
  }
}


