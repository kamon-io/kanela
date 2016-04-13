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

import akka.actor.{ ActorRef, InternalActorRef }
import akka.pattern.AskTimeoutException
import akka.util.Timeout
import kamon.agent.libs.net.bytebuddy.asm.Advice._
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers._
import kamon.agent.scala.KamonInstrumentation
import kamon.akka.AkkaExtension
import kamon.akka.AskPatternTimeoutWarningSettings.{ Heavyweight, Lightweight, Off }
import kamon.trace.Tracer
import kamon.util.SameThreadExecutionContext
import kamon.util.logger.LazyLogger

import scala.compat.Platform.EOL
import scala.concurrent.Future

class AskPatternInstrumentation extends KamonInstrumentation {

  val AskMethod: Junction[MethodDescription] = named("$qmark$extension")

  /**
   * Instrument:
   *
   * akka.pattern.AskableActorRef::?
   *
   */
  forTargetType("akka.pattern.AskableActorRef$") { builder ⇒
    builder
      .withAdvisorFor(AskMethod, classOf[AskMethodAdvisor])
      .build()
  }
}

/**
 * Advisor for akka.pattern.AskableActorRef::?
 */
class AskMethodAdvisor
object AskMethodAdvisor {

  private val log = LazyLogger(getClass)

  class StackTraceCaptureException extends Throwable

  case class SourceLocation(declaringType: String, method: String)
  object SourceLocation {
    def apply(origin: String) = {
      val methodDescription = origin.split(" ")
      new SourceLocation(methodDescription(0), methodDescription(1))
    }
  }

  @OnMethodExit
  def onExit(@Origin("#t #m") origin: String,
    @Return future: Future[AnyRef],
    @Argument(0) actor: ActorRef,
    @Argument(2) timeout: Timeout) = {

    // the AskPattern will only work for InternalActorRef's with these conditions.
    actor match {
      case internalActorRef: InternalActorRef ⇒
        if (!internalActorRef.isTerminated && timeout.duration.length > 0 && Tracer.currentContext.nonEmpty) {
          AkkaExtension.askPatternTimeoutWarning match {
            case Off         ⇒
            case Lightweight ⇒ hookLightweightWarning(future, SourceLocation(origin), actor)
            case Heavyweight ⇒ hookHeavyweightWarning(future, new StackTraceCaptureException, actor)
          }
        }
      case _ ⇒
    }
  }

  def ifAskTimeoutException(code: ⇒ Unit): PartialFunction[Throwable, Unit] = {
    case tmo: AskTimeoutException ⇒ code
    case _                        ⇒
  }

  def hookLightweightWarning(future: Future[AnyRef], sourceLocation: SourceLocation, actor: ActorRef): Unit = {
    val locationString = Option(sourceLocation)
      .map(location ⇒ s"${location.declaringType}:${location.method}")
      .getOrElse("<unknown position>")

    future.onFailure(ifAskTimeoutException {
      log.warn(s"Timeout triggered for ask pattern to actor [${actor.path.name}] at [$locationString]")
    })(SameThreadExecutionContext)
  }

  def hookHeavyweightWarning(future: Future[AnyRef], captureException: StackTraceCaptureException, actor: ActorRef): Unit = {
    val locationString = captureException.getStackTrace.drop(3).mkString("", EOL, EOL)

    future.onFailure(ifAskTimeoutException {
      log.warn(s"Timeout triggered for ask pattern to actor [${actor.path.name}] at [$locationString]")
    })(SameThreadExecutionContext)
  }
}

