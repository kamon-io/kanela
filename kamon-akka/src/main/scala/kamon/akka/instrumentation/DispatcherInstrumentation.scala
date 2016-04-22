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

import akka.kamon.instrumentation.advisor._
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers._
import kamon.agent.scala.KamonInstrumentation
import kamon.akka.instrumentation.mixin.{ ActorSystemAwareMixin, LookupDataAwareMixin }

class DispatcherInstrumentation extends KamonInstrumentation {

  /**
   * Instrument:
   *
   *  akka.dispatch.Dispatchers::lookup
   *
   */

  val LookupMethod: Junction[MethodDescription] = named("lookup")

  forTargetType("akka.dispatch.Dispatchers") { builder ⇒
    builder
      .withMixin(classOf[ActorSystemAwareMixin])
      .withAdvisorFor(LookupMethod, classOf[LookupMethodAdvisor])
      .build()
  }

  /**
   * Instrument:
   *
   * akka.actor.ActorSystemImpl::start
   *
   */

  val StartMethod: Junction[MethodDescription] = named("start")

  forTargetType("akka.actor.ActorSystemImpl") { builder ⇒
    builder
      .withAdvisorFor(StartMethod, classOf[StartMethodAdvisor])
      .build()
  }

  /**
   * Instrument:
   *
   * akka.dispatch.ExecutorServiceFactory+::constructor
   *
   * Mix:
   *
   *
   */

  val Constructor: Junction[MethodDescription] = isConstructor()
  val CreateExecutorServiceMethod: Junction[MethodDescription] = named("createExecutorService")

  forSubtypeOf("akka.dispatch.ExecutorServiceFactory") { builder ⇒
    builder
      .withAdvisorFor(Constructor, classOf[ExecutorServiceFactoryConstructorAdvisor])
      .withAdvisorFor(CreateExecutorServiceMethod, classOf[CreateExecutorServiceAdvisor])
      .build()
  }

  /**
   * Instrument:
   *
   * akka.dispatch.ExecutorServiceDelegate::constructor
   * akka.dispatch.ExecutorServiceDelegate::copy
   * akka.dispatch.ExecutorServiceDelegate::shutdown
   *
   * Mix:
   *
   *
   */

  val CopyMethod: Junction[MethodDescription] = named("copy")
  val ShutdownMethod: Junction[MethodDescription] = named("shutdown")

  forSubtypeOf("akka.dispatch.ExecutorServiceDelegate") { builder ⇒
    builder
      .withMixin(classOf[LookupDataAwareMixin])
      .withAdvisorFor(Constructor, classOf[LazyExecutorServiceDelegateConstructorAdvisor])
      .withAdvisorFor(CopyMethod, classOf[CopyMethodAdvisor])
      .withAdvisorFor(ShutdownMethod, classOf[ShutdownMethodAdvisor])
      .build()
  }

  /**
   * Instrument:
   *
   * akka.routing.BalancingPool::newRoutee
   */

  val NewRouteeMethod: Junction[MethodDescription] = named("newRoutee")

  forTargetType("akka.routing.BalancingPool") { builder ⇒
    builder
      .withAdvisorFor(NewRouteeMethod, classOf[NewRouteeMethodAdvisor])
      .build()
  }

  forSubtypeOf("akka.dispatch.ExecutorServiceFactory") { builder ⇒
    builder
      .withMixin(classOf[LookupDataAwareMixin])
      .build()
  }
}
