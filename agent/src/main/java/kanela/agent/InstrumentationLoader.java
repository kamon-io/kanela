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

import io.vavr.collection.List;
import io.vavr.control.Option;
import kanela.agent.api.instrumentation.KanelaInstrumentation;
import kanela.agent.api.instrumentation.listener.InstrumentationRegistryListener;
import kanela.agent.builder.AgentInstaller;
import kanela.agent.builder.KanelaFileTransformer;
import kanela.agent.util.conf.KanelaConfiguration;
import kanela.agent.util.log.Logger;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.jar.Manifest;

import static java.text.MessageFormat.format;

public class InstrumentationLoader {

    /**
     * Load from the current classpath all defined instrumentations {@link KanelaInstrumentation}.
     *
     * @param instrumentation {@link Instrumentation}
     * @param ctxClassloader {@link ClassLoader}
     * @param configuration {@link KanelaConfiguration}
     * @return a list of {@link KanelaFileTransformer}
     */
    public static List<KanelaFileTransformer> load(Instrumentation instrumentation, ClassLoader ctxClassloader, KanelaConfiguration configuration) {
        return configuration.getAgentModules().map((moduleConfiguration) -> {
            Logger.info(() -> format("Loading {0} ",  moduleConfiguration.getName()));
            return moduleConfiguration.getInstrumentations()
                    .flatMap(instrumentationClassName -> loadInstrumentation(configuration, moduleConfiguration, instrumentationClassName, ctxClassloader))
                    .filter(kanelaInstrumentation -> kanelaInstrumentation.isEnabled(moduleConfiguration))
                    .sortBy(KanelaInstrumentation::order)
                    .flatMap(kanelaInstrumentation -> kanelaInstrumentation.collectTransformations(moduleConfiguration, instrumentation))
                    .foldLeft(AgentInstaller.from(configuration, moduleConfiguration, instrumentation), AgentInstaller::addTypeTransformation)
                    .install();
        });
    }

    private static <T> Option<String> getModuleVersion(Class<T> clazz) {
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (!classPath.startsWith("jar")) { // Class not from JAR
            return Option.none();
        } else {
            try {
                String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
                Manifest m = new Manifest(new URL(manifestPath).openStream());
                return Option.some(m.getMainAttributes().getValue("Implementation-Version"));
            } catch (Exception e) {
                return Option.none();
            }
        }
    }

    private static Option<KanelaInstrumentation> loadInstrumentation(KanelaConfiguration configuration, KanelaConfiguration.ModuleConfiguration moduleConfiguration, String instrumentationClassName, ClassLoader classLoader) {
        Logger.info(() -> format(" ==> Loading {0} ", instrumentationClassName));
        try {
            Class<?> instrumentationClass = Class.forName(instrumentationClassName, true, classLoader);
            if (configuration.getInstrumentationRegistryConfig().isEnabled()) {
                InstrumentationRegistryListener.instance().registerModuleVersion(moduleConfiguration.getKey(), getModuleVersion(instrumentationClass));
            }
            return Option.some((KanelaInstrumentation) instrumentationClass.newInstance());
        } catch (Throwable cause) {
            Logger.warn(() -> format("Error trying to load Instrumentation: {0} with error: {1}", instrumentationClassName, cause));
            return Option.none();
        }
    }
}
