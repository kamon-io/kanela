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

package kanela.agent.builder;

import kanela.agent.api.instrumentation.TypeTransformation;
import kanela.agent.api.instrumentation.listener.DebugInstrumentationListener;
import kanela.agent.api.instrumentation.listener.DefaultInstrumentationListener;
import kanela.agent.api.instrumentation.listener.InstrumentationRegistryListener;
import kanela.agent.api.instrumentation.listener.dumper.ClassDumperListener;
import kanela.agent.cache.PoolStrategyCache;
import kanela.agent.resubmitter.PeriodicResubmitter;
import kanela.agent.util.ListBuilder;
import kanela.agent.util.PatternMatcher;
import kanela.agent.util.conf.KanelaConfiguration;
import kanela.agent.util.log.Logger;
import lombok.Value;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.InjectionStrategy;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.NameMatcher;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;

import static kanela.agent.util.classloader.ClassLoaderNameMatcher.*;
import static net.bytebuddy.matcher.ElementMatchers.*;

@Value(staticConstructor = "from")
class KanelaAgentBuilder {

    KanelaConfiguration config;
    KanelaConfiguration.ModuleConfiguration moduleDescription;
    Instrumentation instrumentation;

    private static final PoolStrategyCache poolStrategyCache = PoolStrategyCache.instance();
    final ListBuilder<TypeTransformation> typeTransformations = ListBuilder.builder();

    public void addTypeTransformation(TypeTransformation typeTransformation) {
        InstrumentationRegistryListener.instance().register(moduleDescription, typeTransformation);
        typeTransformations.add(typeTransformation);
    }

    AgentBuilder build() {
        return typeTransformations.build().foldLeft(newAgentBuilder(), (agent, typeTransformation) -> {
            val transformers = new ArrayList<AgentBuilder.Transformer>();
            transformers.addAll(typeTransformation.getBridges());
            transformers.addAll(typeTransformation.getMixins());
            transformers.addAll(typeTransformation.getTransformations());

            for (AgentBuilder.Transformer transformer : transformers) {
                agent  = agent
                        .type(typeTransformation.getElementMatcher().get(), RefinedClassLoaderMatcher.from(typeTransformation.getClassLoaderRefiner()))
                        .transform(transformer);
             }
             return agent;
        });
    }

    private AgentBuilder newAgentBuilder() {
        val byteBuddy = new ByteBuddy()
            .with(TypeValidation.of(config.isDebugMode()))
            .with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE);

        AgentBuilder agentBuilder = new AgentBuilder.Default(byteBuddy)
                .with(poolStrategyCache);


        agentBuilder = withRetransformationForRuntime(agentBuilder);
        agentBuilder = withBootstrapAttaching(agentBuilder);
        agentBuilder = withIgnore(agentBuilder);

        return agentBuilder
                .with(DefaultInstrumentationListener.instance())
                .with(additionalListeners());
}

    private AgentBuilder withRetransformationForRuntime(AgentBuilder agentBuilder) {
        if (config.isAttachedInRuntime() || moduleDescription.isStoppable() || moduleDescription.shouldInjectInBootstrap()) {
            Logger.info(() -> "Retransformation Strategy activated for: " + moduleDescription.getName());

            if(moduleDescription.isDisableClassFormatChanges())
                agentBuilder = agentBuilder.disableClassFormatChanges(); // enable restrictions imposed by most VMs and also HotSpot.

            agentBuilder = agentBuilder
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .withResubmission(PeriodicResubmitter.instance()).resubmitOnError();
        }
        return agentBuilder;
    }

    private AgentBuilder withBootstrapAttaching(AgentBuilder agentBuilder) {
        if(moduleDescription.shouldInjectInBootstrap()){
            Logger.info(() -> "Bootstrap Injection activated.");
            agentBuilder = agentBuilder.with(new InjectionStrategy.UsingUnsafe.OfFactory(ClassInjector.UsingUnsafe.Factory.resolve(instrumentation)));
        }
        return agentBuilder;
    }

    private AgentBuilder withIgnore(AgentBuilder agentBuilder) {
        AgentBuilder.Ignored builder = agentBuilder.ignore(ignoreMatches())
                .or(moduleExcludes())
                .or(any(), isExtensionClassLoader())
                .or(any(), isKanelaClassLoader())
                .or(any(), isGroovyClassLoader())
                .or(any(), isSBTClassLoader())
                .or(any(), isSBTPluginClassLoader())
                .or(any(), isSBTCompilerClassLoader())
                .or(any(), isSBTCachedClassLoader())
                .or(any(), isLagomClassLoader())
                .or(any(), isLagomServiceLocatorClassLoader())
                .or(any(), isReflectionClassLoader());

        if (moduleDescription.shouldInjectInBootstrap()) return builder;
        return builder.or(any(), isBootstrapClassLoader());
    }

    private AgentBuilder.Listener additionalListeners() {
        val listeners = new ArrayList<AgentBuilder.Listener>();
        if (config.getDump().isDumpEnabled()) listeners.add(ClassDumperListener.instance());
        if (config.getDebugMode()) listeners.add(DebugInstrumentationListener.instance());
        if (config.getInstrumentationRegistryConfig().isEnabled()) listeners.add(InstrumentationRegistryListener.instance());
        return new AgentBuilder.Listener.Compound(listeners);
    }

    private ElementMatcher.Junction<NamedElement> ignoreMatches() {
        final String withinPackage = moduleDescription.getWithinPackage();
        if (withinPackage.isEmpty()) {
            return any();
        } else {
            return not(new NameMatcher<>(new PatternMatcher(withinPackage)));
        }
    }

    private ElementMatcher.Junction<NamedElement> moduleExcludes() {
        final String moduleExcludes = moduleDescription.getExcludePackage();
        if (moduleExcludes.isEmpty()) {
            return none();
        } else {
            return new NameMatcher<>(new PatternMatcher(moduleExcludes));
        }
    }
}
