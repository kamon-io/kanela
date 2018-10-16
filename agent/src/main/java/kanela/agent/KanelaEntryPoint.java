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

import kanela.agent.api.instrumentation.replacer.ClassReplacer;
import kanela.agent.circuitbreaker.SystemThroughputCircuitBreaker;
import kanela.agent.reinstrument.Reinstrumenter;
import kanela.agent.util.BootstrapInjector;
import kanela.agent.util.ExtensionLoader;
import kanela.agent.util.banner.KanelaBanner;
import kanela.agent.util.classloader.KanelaClassLoader;
import kanela.agent.util.conf.KanelaConfiguration;
import kanela.agent.util.jvm.OldGarbageCollectorListener;
import lombok.Value;
import lombok.val;

import java.lang.instrument.Instrumentation;

import static kanela.agent.util.Execution.runWithTimeSpent;

@Value
public class KanelaEntryPoint {
    /**
     * Kanela entry point.
     *
     * @param arguments Agent argument list
     * @param instrumentation {@link Instrumentation}
     */
    private static void start(final String arguments, final Instrumentation instrumentation, boolean isRuntimeAttach) {
        runWithTimeSpent(() -> {
            KanelaClassLoader.from(instrumentation).use(kanelaClassLoader -> {
                BootstrapInjector.injectJar(instrumentation, "bootstrap");
                val configuration = KanelaConfiguration.instance();

                if(isRuntimeAttach) {
                    configuration.runtimeAttach();
                }

                KanelaBanner.show(configuration);

                ExtensionLoader.attach(arguments, instrumentation);

                val transformers = InstrumentationLoader.load(instrumentation, kanelaClassLoader, configuration);
                Reinstrumenter.attach(instrumentation, configuration, transformers);
                OldGarbageCollectorListener.attach(configuration.getOldGarbageCollectorConfig());
                SystemThroughputCircuitBreaker.attach(configuration.getCircuitBreakerConfig());
                ClassReplacer.attach(instrumentation, configuration.getClassReplacerConfig());
            });
        });
    }

    public static void premain(final String arguments, final Instrumentation instrumentation) {
        start(arguments, instrumentation, false);
    }

    public static void agentmain(final String arguments, final Instrumentation instrumentation) {
        start(arguments, instrumentation, true);
    }
}
