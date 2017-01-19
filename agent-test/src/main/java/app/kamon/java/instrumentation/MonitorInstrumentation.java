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

package app.kamon.java.instrumentation;

import app.kamon.java.instrumentation.advisor.FakeWorkerAdvisor;
import app.kamon.java.instrumentation.mixin.MonitorMixin;
import kamon.agent.api.instrumentation.KamonInstrumentation;

import static kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers.named;

public class MonitorInstrumentation extends KamonInstrumentation {

    public MonitorInstrumentation() {
        forTargetType(() -> "app.kamon.java.FakeWorker", builder ->
            builder.withMixin(() -> MonitorMixin.class)
                   .withAdvisorFor(named("heavyTask"), () -> FakeWorkerAdvisor.class)
                   .withAdvisorFor(named("lightTask"), () -> FakeWorkerAdvisor.class)
                   .build()
        );
    }

}
