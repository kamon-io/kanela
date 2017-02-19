/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

import akka.kamon.instrumentation.advisor._
import kamon.agent.scala.KamonInstrumentation
import kamon.akka.instrumentation.mixin.{ActorInstrumentationMixin, RoutedActorCellInstrumentationMixin}

class ActorInstrumentation extends KamonInstrumentation {

  /**
   * Instrument:
   *
   * akka.actor.ActorCell::constructor
   * akka.actor.ActorCell::invoke
   * akka.actor.ActorCell::invokeAll
   * akka.actor.ActorCell::sendMessage
   * akka.actor.ActorCell::stop
   *
   * Mix:
   *
   * akka.actor.ActorCell with kamon.akka.instrumentation.mixin.ActorInstrumentationAware
   *
   */
  forTargetType("akka.actor.ActorCell") { builder ⇒
    builder
      .withMixin(classOf[ActorInstrumentationMixin])
      .withAdvisorFor(isConstructor(), classOf[ActorCellConstructorAdvisor])
      .withAdvisorFor(named("invoke"), classOf[InvokeMethodAdvisor])
      .withAdvisorFor(named("invokeAll$1"), classOf[InvokeAllMethodAdvisor])
      .withAdvisorFor(named("handleInvokeFailure"), classOf[HandleInvokeFailureMethodAdvisor])
      .withAdvisorFor(named("sendMessage").and(takesArguments(1)), classOf[SendMessageMethodAdvisor])
      .withAdvisorFor(named("stop"), classOf[StopMethodAdvisor])
      .build()
  }

  /**
   * Instrument:
   *
   * akka.actor.UnstartedCell::constructor
   * akka.actor.UnstartedCell::sendMessage
   * akka.actor.UnstartedCell::replaceWith
   *
   * Mix:
   *
   * akka.actor.UnstartedCell with kamon.akka.instrumentation.mixin.ActorInstrumentationAware
   *
   */
  forTargetType("akka.actor.UnstartedCell") { builder ⇒
    builder
      .withMixin(classOf[ActorInstrumentationMixin])
      .withAdvisorFor(isConstructor(), classOf[RepointableActorCellConstructorAdvisor])
      .withAdvisorFor(named("sendMessage").and(takesArguments(1)), classOf[SendMessageMethodAdvisor])
      .withAdvisorFor(named("replaceWith"), classOf[ParameterWrapperAdvisor])
      .build()
  }

  /**
   * Instrument:
   *
   * akka.routing.RoutedActorCell::constructor
   * akka.routing.RoutedActorCell::sendMessage
   *
   * Mix:
   *
   * akka.routing.RoutedActorCell with kamon.akka.instrumentation.mixin.RouterInstrumentationAware
   *
   */
  forTargetType("akka.routing.RoutedActorCell") { builder ⇒
    builder
      .withMixin(classOf[RoutedActorCellInstrumentationMixin])
      .withAdvisorFor(isConstructor(), classOf[RoutedActorCellConstructorAdvisor])
      .withAdvisorFor(named("sendMessage").and(takesArguments(1)), classOf[SendMessageMethodAdvisorForRouter])
      .build()
  }
}