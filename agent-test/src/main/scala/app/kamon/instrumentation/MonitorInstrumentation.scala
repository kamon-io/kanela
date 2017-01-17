/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package app.kamon.instrumentation

import app.kamon.instrumentation.advisor.GreetingsKamonTeamAdvisor
import app.kamon.instrumentation.mixin.MonitorMixin
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers.named
import kamon.agent.scala

class MonitorInstrumentation extends scala.KamonInstrumentation {

  forTargetType("app.kamon.GreetingsKamonTeam") { builder ⇒
    builder
      .withMixin(classOf[MonitorMixin])
      .withAdvisorFor(named("salute"), classOf[GreetingsKamonTeamAdvisor])
      .withAdvisorFor(named("welcome"), classOf[GreetingsKamonTeamAdvisor])
      .build()
  }
}

