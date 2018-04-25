/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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

package kanela.agent;

import kanela.agent.circuitbreaker.SystemThroughputCircuitBreaker;
import kanela.agent.reinstrument.Reinstrumenter;
import kanela.agent.util.BootstrapInjector;
import kanela.agent.util.ExtensionLoader;
import kanela.agent.util.banner.KanelaBanner;
import kanela.agent.util.conf.KanelaConfiguration;
import kanela.agent.util.jvm.OldGarbageCollectorListener;
import kanela.agent.util.log.Logger;
import lombok.Value;
import lombok.val;

import java.lang.instrument.Instrumentation;

import static kanela.agent.util.LatencyUtils.withTimeSpent;

@Value
public class KanelaEntryPoint {
    /**
     * Kanela entry point.
     *
     * @param arguments Agent argument list
     * @param instrumentation {@link Instrumentation}
     */
    private static void start(String arguments, Instrumentation instrumentation) {
        withTimeSpent(() -> {
            val configuration = KanelaConfiguration.instance();
            KanelaBanner.show(configuration);
            BootstrapInjector.injectJar(instrumentation, "bootstrap");
            ExtensionLoader.attach(arguments, instrumentation);

            val transformers = InstrumentationLoader.load(instrumentation, configuration);
            Reinstrumenter.attach(instrumentation, configuration, transformers);
            OldGarbageCollectorListener.attach(configuration.getOldGarbageCollectorConfig());
            SystemThroughputCircuitBreaker.attach(configuration.getCircuitBreakerConfig());

        }, (timeSpent) -> Logger.info(() -> "Startup completed in " + timeSpent + " ms"));
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        start(arguments, instrumentation);
    }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        KanelaConfiguration.instance().runtimeAttach();
        premain(arguments, instrumentation);
    }
}
