/*
 * =========================================================================================
 * Copyright © 2013-2019 the kamon project <http://kamon.io/>
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
import io.vavr.control.Try;
import kanela.agent.api.instrumentation.InstrumentationBuilder;
import kanela.agent.builder.AgentInstaller;
import kanela.agent.builder.KanelaFileTransformer;
import kanela.agent.util.conf.KanelaConfiguration;
import kanela.agent.util.log.Logger;

import java.lang.instrument.Instrumentation;

import static java.text.MessageFormat.format;

public class InstrumentationLoader {

    /**
     * Load from the current classpath all defined instrumentations {@link InstrumentationBuilder}.
     *
     * @param instrumentation {@link Instrumentation}
     * @param ctxClassloader  {@link ClassLoader}
     * @param configuration   {@link KanelaConfiguration}
     * @return a list of {@link KanelaFileTransformer}
     */
    public static List<KanelaFileTransformer> load(Instrumentation instrumentation, ClassLoader ctxClassloader, KanelaConfiguration configuration) {
        return configuration.getAgentModules().map((moduleConfiguration) -> {
            Logger.info(() -> format("Loading {0}", moduleConfiguration.getName()));
            return moduleConfiguration.getInstrumentations()
                    .flatMap(instrumentationClassName -> loadInstrumentation(instrumentationClassName, ctxClassloader))
                    .filter(kanelaInstrumentation -> kanelaInstrumentation.isEnabled(moduleConfiguration))
                    .sortBy(InstrumentationBuilder::order)
                    .flatMap(kanelaInstrumentation -> kanelaInstrumentation.collectTransformations(moduleConfiguration, instrumentation))
                    .foldLeft(AgentInstaller.from(configuration, moduleConfiguration, instrumentation), AgentInstaller::addTypeTransformation)
                    .install();
        });
    }

    private static Option<InstrumentationBuilder> loadInstrumentation(String instrumentationClassName, ClassLoader classLoader) {
        return Try.of(() -> {
            Logger.info(() -> format(" ==> Loading {0} ", instrumentationClassName));
            return (InstrumentationBuilder) Class.forName(instrumentationClassName, true, classLoader).getConstructors()[0].newInstance();
        }).onFailure((cause) -> Logger.warn(() -> format("Error trying to load Instrumentation: {0} with error: {1}", instrumentationClassName, cause))
        ).toOption();
    }
}
