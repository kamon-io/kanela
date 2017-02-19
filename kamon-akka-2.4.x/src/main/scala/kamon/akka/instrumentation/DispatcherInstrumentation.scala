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
import kamon.akka.instrumentation.mixin.{ActorSystemAwareMixin, LookupDataAwareMixin}

class DispatcherInstrumentation extends KamonInstrumentation {

  /**
   * Instrument:
   *
   *  akka.dispatch.Dispatchers::lookup
   *
   */
  forTargetType("akka.dispatch.Dispatchers") { builder ⇒
    builder
      .withMixin(classOf[ActorSystemAwareMixin])
      .withAdvisorFor(named("lookup"), classOf[LookupMethodAdvisor])
      .build()
  }

  /**
   * Instrument:
   *
   * akka.actor.ActorSystemImpl::start
   *
   */
  forTargetType("akka.actor.ActorSystemImpl") { builder ⇒
    builder
      .withAdvisorFor(named("start"), classOf[StartMethodAdvisor])
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
  forSubtypeOf("akka.dispatch.ExecutorServiceFactory") { builder ⇒
    builder
      .withMixin(classOf[LookupDataAwareMixin])
      .withAdvisorFor(isConstructor(), classOf[ExecutorServiceFactoryConstructorAdvisor])
      .withAdvisorFor(named("createExecutorService"), classOf[CreateExecutorServiceAdvisor])
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
  forSubtypeOf("akka.dispatch.ExecutorServiceDelegate") { builder ⇒
    builder
      .withMixin(classOf[LookupDataAwareMixin])
      .withAdvisorFor(isConstructor(), classOf[LazyExecutorServiceDelegateConstructorAdvisor])
      .withAdvisorFor(named("copy"), classOf[CopyMethodAdvisor])
      .withAdvisorFor(named("shutdown"), classOf[ShutdownMethodAdvisor])
      .build()
  }

  /**
   * Instrument:
   *
   * akka.routing.BalancingPool::newRoutee
   */
  forTargetType("akka.routing.BalancingPool") { builder ⇒
    builder
      .withAdvisorFor(named("newRoutee"), classOf[NewRouteeMethodAdvisor])
      .build()
  }
}
