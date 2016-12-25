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

import javaslang.control.Try;
import kamon.agent.api.instrumentation.KamonInstrumentation;
import kamon.agent.builder.Agents;
import kamon.agent.util.log.LazyLogger;

import lombok.val;
import java.lang.instrument.Instrumentation;

import static java.text.MessageFormat.format;

public class InstrumentationLoader {

    /**
     * @param instrumentation: provided by JVM
     */
    public static void load(Instrumentation instrumentation, AgentConfiguration config) {
        config.getInstrumentations()
                .map(InstrumentationLoader::loadInstrumentation)
                .sortBy(KamonInstrumentation::order)
                .flatMap(KamonInstrumentation::collectTransformations)
                .foldLeft(Agents.from(config), Agents::addTypeTransformation)
                .install(instrumentation);
    }

    private static KamonInstrumentation loadInstrumentation(String instrumentationClassName) {
        LazyLogger.info(() -> format("Loading {0}...", instrumentationClassName));
        return Try.of(() -> (KamonInstrumentation) Class.forName(instrumentationClassName, true, InstrumentationLoader.class.getClassLoader()).newInstance())
                  .getOrElseThrow((cause) -> new RuntimeException(format("Error trying to load Instrumentation {0}", instrumentationClassName), cause));
    }
}
