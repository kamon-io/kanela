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

package kamon.agent;

import kamon.agent.builder.KamonAgentFileTransformer;
import kamon.agent.circuitbreaker.SystemThroughputCircuitBreaker;
import kamon.agent.reinstrument.Reinstrumenter;
import kamon.agent.util.banner.AgentBanner;
import kamon.agent.util.conf.AgentConfiguration;
import kamon.agent.util.jvm.OldGarbageCollectorListener;
import kamon.agent.util.log.LazyLogger;
import lombok.Value;
import lombok.val;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import static kamon.agent.util.AgentUtil.timed;

@Value
public class AgentEntryPoint {
    private static List<KamonAgentFileTransformer> filesTransformers = new ArrayList<>();

    private static void start(String args, Instrumentation instrumentation) {
        val timeSpent = timed(() -> {
            val configuration = AgentConfiguration.instance();
            AgentBanner.show(configuration);
            OldGarbageCollectorListener.attach(configuration.getOldGarbageCollectorConfig());
            SystemThroughputCircuitBreaker.attach(configuration.getCircuitBreakerConfig());
            val transformers = InstrumentationLoader.load(instrumentation, configuration);
            Reinstrumenter.attach(instrumentation, configuration, transformers);
        });

        LazyLogger.infoColor(() -> "Startup complete in " + timeSpent + " ms");
    }

    public static void premain(String args, Instrumentation instrumentation) {
        start(args, instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        AgentConfiguration.instance().runtimeAttach();
        premain(args, instrumentation);
    }
}
