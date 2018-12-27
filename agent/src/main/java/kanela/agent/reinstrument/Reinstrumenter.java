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


package kanela.agent.reinstrument;

import io.vavr.collection.List;
import io.vavr.control.Try;
import kanela.agent.api.instrumentation.listener.InstrumentationRegistryListener;
import kanela.agent.broker.EventBroker;
import kanela.agent.broker.Subscribe;
import kanela.agent.builder.KanelaFileTransformer;
import kanela.agent.util.annotation.Experimental;
import kanela.agent.util.conf.KanelaConfiguration;
import kanela.agent.util.log.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

import static java.text.MessageFormat.format;

import lombok.Value;
import lombok.val;

@Value
@Experimental
public class Reinstrumenter {
    Instrumentation instrumentation;
    KanelaConfiguration configuration;
    List<KanelaFileTransformer> transformers;

    public static void attach(Instrumentation instrumentation, KanelaConfiguration configuration, List<KanelaFileTransformer> transformers) {
        Try.of(() -> new Reinstrumenter(instrumentation, configuration, transformers))
                .andThen(() -> Logger.info(() -> format("Reinstrumenter activated.")))
                .andThen(reinstrumenter -> EventBroker.instance().add(reinstrumenter))
                .andThen(() -> Logger.debug(() -> format("Reinstrumenter is listening for Reinstrumentation Events.")))
                .onFailure((cause) -> Logger.error(() -> format("Error when trying to activate Reinstrumenter."), cause));
    }

    @Subscribe
    public void onStopModules(ReinstrumentationProtocol.StopModules stopEvent) {
        InstrumentationRegistryListener.instance().stop();
        Logger.warn(() -> "Trying to stop modules.....");
        val stoppables = this.transformers.filter(KanelaFileTransformer::isStoppable)
                                          .map(KanelaFileTransformer::getClassFileTransformer)
                                          .map(transformer -> transformer.reset(this.instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION));

        if(stoppables.forAll(s -> s.equals(true))) Logger.warn(() -> "All modules are been stopped.");
        else Logger.warn(() -> "Error trying stop some modules.");
    }

    @Subscribe
    public void onRestartModules(ReinstrumentationProtocol.RestartModules restartEvent) {
        Logger.warn(() -> "Trying to reapply the removed transformations...");
        this.transformers.filter(KanelaFileTransformer::isStoppable)
                         .map(KanelaFileTransformer::getAgentBuilder)
                         .forEach(transformer -> transformer.installOn(this.instrumentation));
    }

    public interface ReinstrumentationProtocol {
        @Value(staticConstructor = "instance")
        class StopModules {}
        @Value(staticConstructor = "instance")
        class RestartModules {}
    }
}


