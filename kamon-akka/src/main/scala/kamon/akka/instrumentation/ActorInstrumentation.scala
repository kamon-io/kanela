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

import akka.actor.dungeon.ChildrenContainer
import akka.actor.{ ActorRef, ActorSystem, ActorSystemImpl, Cell, ChildStats, InternalActorRef, Props }
import akka.dispatch.Envelope
import akka.dispatch.sysmsg.SystemMessage
import akka.kamon.instrumentation.advisor._
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers._
import kamon.agent.scala.KamonInstrumentation
import kamon.akka.instrumentation.mixin.{ ActorInstrumentationMixin, EnvelopeInstrumentationMixin, InstrumentedEnvelope, RoutedActorCellInstrumentationMixin }
import kamon.trace.Tracer

class ActorInstrumentation extends KamonInstrumentation {

  val Constructor: Junction[MethodDescription] = isConstructor()
  val InvokeMethod: Junction[MethodDescription] = named("invoke").and(takesArguments(classOf[Envelope]))
  val SendMessageMethod: Junction[MethodDescription] = named("sendMessage").and(takesArguments(classOf[Envelope]))
  val StopMethod: Junction[MethodDescription] = named("stop")
  val HandleInvokeFailureMethod: Junction[MethodDescription] = named("handleInvokeFailure")
  val ReplaceWitMethod: Junction[MethodDescription] = named("replaceWith")

  /**
   * Mix:
   *
   * akka.dispatch.Envelope with InstrumentedEnvelope
   *
   */
  forTargetType("akka.dispatch.Envelope") { builder ⇒
    builder
      .withMixin(classOf[EnvelopeInstrumentationMixin])
      .build()
  }

  /**
   * Instruments:
   *
   * akka.actor.ActorCell::constructor
   * akka.actor.ActorCell::invoke
   * akka.actor.ActorCell::sendMessage
   * akka.actor.ActorCell::stop
   * akka.actor.ActorCell::handleInvokeFailure
   *
   * Mix:
   *
   * akka.actor.ActorCell with kamon.akka.instrumentation.mixin.ActorInstrumentationAware
   *
   */
  forTargetType("akka.actor.ActorCell") { builder ⇒
    builder
      .withMixin(classOf[ActorInstrumentationMixin])
      .withAdvisorFor(Constructor, classOf[ActorCellConstructorAdvisor])
      .withAdvisorFor(InvokeMethod, classOf[InvokeMethodAdvisor])
      .withAdvisorFor(SendMessageMethod, classOf[SendMessageMethodAdvisor])
      .withAdvisorFor(StopMethod, classOf[StopMethodAdvisor])
      .withAdvisorFor(HandleInvokeFailureMethod, classOf[HandleInvokeFailureMethodAdvisor])
      .build()
  }

  /**
   * Instruments:
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
      .withAdvisorFor(Constructor, classOf[RepointableActorCellConstructorAdvisor])
      .withAdvisorFor(SendMessageMethod, classOf[SendMessageMethodAdvisor])
      .withAdvisorFor(ReplaceWitMethod, classOf[ParameterWrapperAdvisor])
      .build()
  }

  /**
   * Instruments:
   *
   * akka.dispatch.RoutedActorCell::constructor
   * akka.dispatch.RoutedActorCell::sendMessage
   *
   * Mix:
   *
   * akka.dispatch.RoutedActorCell with kamon.akka.instrumentation.mixin.RouterInstrumentationAware
   *
   */
  forTargetType("akka.dispatch.RoutedActorCell") { builder ⇒
    builder
      .withMixin(classOf[RoutedActorCellInstrumentationMixin])
      .withAdvisorFor(Constructor, classOf[RoutedActorCellConstructorAdvisor])
      .withAdvisorFor(SendMessageMethod, classOf[SendMessageMethodAdvisor])
      .build()
  }
}

object ActorInstrumentation {
  /**
   * Wrap a akka.actor.Cell in order to propagate the current TraceContext when calling sendMessage method
   */
  class TraceContextAwareCell(underlying: Cell) extends Cell {
    def self: ActorRef = underlying.self
    def isTerminated: Boolean = underlying.isTerminated
    def getSingleChild(name: String): InternalActorRef = underlying.getSingleChild(name)
    def stop(): Unit = underlying.stop()
    def numberOfMessages: Int = underlying.numberOfMessages
    def isLocal: Boolean = underlying.isLocal
    def props: Props = underlying.props
    def getChildByName(name: String): Option[ChildStats] = underlying.getChildByName(name)
    def restart(cause: Throwable): Unit = underlying.restart(cause)
    def suspend(): Unit = underlying.suspend()
    def hasMessages: Boolean = underlying.hasMessages
    def systemImpl: ActorSystemImpl = underlying.systemImpl
    def resume(causedByFailure: Throwable): Unit = underlying.resume(causedByFailure)
    def start() = this
    def childrenRefs: ChildrenContainer = underlying.childrenRefs
    def parent: InternalActorRef = underlying.parent
    def system: ActorSystem = underlying.system
    def sendSystemMessage(msg: SystemMessage): Unit = underlying.sendSystemMessage(msg)

    def sendMessage(msg: Envelope): Unit = {
      val envelopeContext = msg.asInstanceOf[InstrumentedEnvelope].envelopeContext()

      Tracer.withContext(envelopeContext.context) {
        underlying.sendMessage(msg)
      }
    }
  }
}