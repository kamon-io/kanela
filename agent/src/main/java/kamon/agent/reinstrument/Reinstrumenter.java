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


package kamon.agent.reinstrument;

import javaslang.collection.List;
import javaslang.control.Try;
import kamon.agent.broker.EventBroker;
import kamon.agent.broker.Subscribe;
import kamon.agent.builder.KamonAgentFileTransformer;
import kamon.agent.util.annotation.Experimental;
import kamon.agent.util.conf.AgentConfiguration;
import kamon.agent.util.log.LazyLogger;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

import static java.text.MessageFormat.format;

@Value
@NonFinal
@Experimental
public class Reinstrumenter {
    Instrumentation instrumentation;
    AgentConfiguration configuration;
    List<KamonAgentFileTransformer> transformers;

    public static void attach(Instrumentation instrumentation, AgentConfiguration configuration, List<KamonAgentFileTransformer> transformers) {
        Try.of(() -> new Reinstrumenter(instrumentation, configuration, transformers))
                .andThen(() -> LazyLogger.infoColor(() -> format("Reinstrumenter was activated.")))
                .andThen(reinstrumenter -> EventBroker.instance().add(reinstrumenter))
                .andThen(() -> LazyLogger.debug(() -> format("Reinstrumenter is listening for Reinstrumentation Events.")))
                .onFailure((cause) -> LazyLogger.errorColor(() -> format("Error when trying to activate Reinstrumenter."), cause));
    }

    @Subscribe
    public void onStopModules(ReinstrumentationProtocol.StopModules stopEvent) {
        LazyLogger.warnColor(() -> "Trying to stop modules.....");
        val stoppables = this.transformers.filter(KamonAgentFileTransformer::isStoppable)
                                          .map(KamonAgentFileTransformer::getClassFileTransformer)
                                          .map(transformer -> transformer.reset(this.instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION));

        if(stoppables.forAll(s -> s.equals(true))) LazyLogger.warnColor(() -> "All modules are been stopped.");
        else LazyLogger.warnColor(() -> "Error trying stop some modules.");
    }

    @Subscribe
    public void onRestartModules(ReinstrumentationProtocol.RestartModules restartEvent) {
        LazyLogger.warnColor(() -> "Trying to reapply the removed transformations...");
        this.transformers.filter(KamonAgentFileTransformer::isStoppable)
                         .map(KamonAgentFileTransformer::getAgentBuilder)
                         .map(transformer -> transformer.installOn(this.instrumentation));
    }

    public interface ReinstrumentationProtocol {
        @Value(staticConstructor = "instance")
        class StopModules {}
        @Value(staticConstructor = "instance")
        class RestartModules {}
    }
}


