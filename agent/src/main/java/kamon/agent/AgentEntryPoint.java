/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

package kamon.agent;

import kamon.agent.circuitbreaker.SystemThroughputCircuitBreaker;
import kamon.agent.reinstrument.Reinstrumenter;
import kamon.agent.util.banner.AgentBanner;
import kamon.agent.util.conf.AgentConfiguration;
import kamon.agent.util.jvm.OldGarbageCollectorListener;
import kamon.agent.util.log.LazyLogger;
import lombok.Value;
import lombok.val;

import java.lang.instrument.Instrumentation;

import static kamon.agent.util.LatencyUtils.withTimeSpent;

@Value
public class AgentEntryPoint {
    /**
     * Kamon Agent entry point.
     *
     * @param args Agent argument list
     * @param instrumentation {@link Instrumentation}
     */
    private static void start(String args, Instrumentation instrumentation) {
        withTimeSpent(() -> {
            val configuration = AgentConfiguration.instance();
            AgentBanner.show(configuration);
            val transformers = InstrumentationLoader.load(instrumentation, configuration);
            //run the other stuff in an async way ;)
            new Thread(() -> {
                Reinstrumenter.attach(instrumentation, configuration, transformers);
                OldGarbageCollectorListener.attach(configuration.getOldGarbageCollectorConfig());
                SystemThroughputCircuitBreaker.attach(configuration.getCircuitBreakerConfig());
            }).start();

        }, (timeSpent) -> LazyLogger.infoColor(() -> "Startup completed in " + timeSpent + " ms"));
    }

    public static void premain(String args, Instrumentation instrumentation) {
        start(args, instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        AgentConfiguration.instance().runtimeAttach();
        premain(args, instrumentation);
    }
}
