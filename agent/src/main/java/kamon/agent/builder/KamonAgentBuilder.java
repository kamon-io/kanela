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

package kamon.agent.builder;

import io.vavr.Function1;
import kamon.agent.api.instrumentation.TypeTransformation;
import kamon.agent.cache.PoolStrategyCache;
import kamon.agent.resubmitter.PeriodicResubmitter;
import kamon.agent.util.ListBuilder;
import kamon.agent.util.conf.AgentConfiguration;
import kamon.agent.util.conf.AgentConfiguration.ModuleConfiguration;
import kamon.agent.util.log.LazyLogger;
import lombok.experimental.var;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;

import static net.bytebuddy.matcher.ElementMatchers.nameMatches;
import static net.bytebuddy.matcher.ElementMatchers.not;


abstract class KamonAgentBuilder {

    private static final Function1<ModuleConfiguration, ElementMatcher.Junction<NamedElement>> configuredMatcherList = ignoredMatcherList().memoized();
    private static final PoolStrategyCache poolStrategyCache = PoolStrategyCache.instance();
    final ListBuilder<TypeTransformation> typeTransformations = ListBuilder.builder();

    protected abstract AgentBuilder newAgentBuilder(AgentConfiguration config, ModuleConfiguration moduleDescription, Instrumentation instrumentation);
    protected abstract void addTypeTransformation(TypeTransformation typeTransformation);

    AgentBuilder from(AgentConfiguration config, ModuleConfiguration moduleDescription, Instrumentation instrumentation) {
        val byteBuddy = new ByteBuddy().with(TypeValidation.of(config.isDebugMode()))
                                       .with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE);

        var agentBuilder = new AgentBuilder.Default(byteBuddy)
                                                    .with(poolStrategyCache);

        if (config.isAttachedInRuntime() || moduleDescription.isStoppable()) {
            LazyLogger.infoColor(() -> "Retransformation Strategy activated.");
            agentBuilder = agentBuilder.disableClassFormatChanges()
                                       .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                                       .withResubmission(PeriodicResubmitter.instance());
        }

        if(moduleDescription.shouldInjectInBootstrap()){
            LazyLogger.infoColor(() -> "Bootstrap Injection activated.");
            agentBuilder = agentBuilder.enableBootstrapInjection(instrumentation, moduleDescription.getTempDir());
        }

        return agentBuilder.ignore(configuredMatcherList.apply(moduleDescription));
    }

    AgentBuilder build(AgentConfiguration config, ModuleConfiguration moduleDescription, Instrumentation instrumentation) {
            return typeTransformations.build().foldLeft(newAgentBuilder(config, moduleDescription, instrumentation), (agent, typeTransformation) -> {
                val transformers = new ArrayList<AgentBuilder.Transformer>();
                transformers.addAll(typeTransformation.getBridges().toJavaList());
                transformers.addAll(typeTransformation.getMixins().toJavaList());
                transformers.addAll(typeTransformation.getTransformations().toJavaList());
                return agent.type(typeTransformation.getElementMatcher().get())
                            .transform(new AgentBuilder.Transformer.Compound(transformers));
            });
    }

    private static Function1<ModuleConfiguration,ElementMatcher.Junction<NamedElement>> ignoredMatcherList() {
        return (moduleDescription) -> not(nameMatches(moduleDescription.getWithinPackage()));
    }

    protected String agentName() {
        return getClass().getSimpleName();
    }
}
