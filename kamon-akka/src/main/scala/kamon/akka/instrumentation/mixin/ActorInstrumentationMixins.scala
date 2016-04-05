package kamon.akka.instrumentation.mixin

import akka.kamon.instrumentation.ActorMonitor

trait ActorInstrumentationAware {
  def actorInstrumentation: ActorMonitor
  def setActorInstrumentation(ai: ActorMonitor): Unit
}

class ActorInstrumentationMixin extends ActorInstrumentationAware {
  @volatile private var _ai: ActorMonitor = _

  def setActorInstrumentation(ai: ActorMonitor): Unit = _ai = ai
  def actorInstrumentation: ActorMonitor = _ai
}
