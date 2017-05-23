package kamon.akka.instrumentation.mixin

import akka.actor.ActorSystem

class ActorSystemAwareMixin extends ActorSystemAware

trait ActorSystemAware {
  @volatile var actorSystem: ActorSystem = _
}
