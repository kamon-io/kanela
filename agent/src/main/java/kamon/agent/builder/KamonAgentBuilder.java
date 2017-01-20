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

import javaslang.Function1;
import javaslang.collection.List;
import kamon.agent.api.instrumentation.TypeTransformation;
import kamon.agent.cache.PoolStrategyCache;
import kamon.agent.util.ListBuilder;
import kamon.agent.util.conf.AgentConfiguration;
import kamon.agent.util.conf.AgentConfiguration.AgentModuleDescription;
import kamon.agent.util.log.LazyLogger;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;

import static kamon.agent.util.matcher.ClassLoaderMatcher.isReflectionClassLoader;
import static kamon.agent.util.matcher.TimedMatcher.withTimeSpent;
import static net.bytebuddy.matcher.ElementMatchers.*;

abstract class KamonAgentBuilder {

    private static final Function1<AgentModuleDescription, ElementMatcher.Junction<NamedElement>> configuredMatcherList = ignoredMatcherList().memoized();
    private static final PoolStrategyCache poolStrategyCache = PoolStrategyCache.instance();
    final ListBuilder<TypeTransformation> typeTransformations = ListBuilder.builder();

    protected abstract AgentBuilder newAgentBuilder(AgentConfiguration config, AgentModuleDescription moduleDescription, Instrumentation instrumentation);
    protected abstract void addTypeTransformation(TypeTransformation typeTransformation);

    AgentBuilder from(AgentConfiguration config, AgentModuleDescription moduleDescription) {
        val byteBuddy = new ByteBuddy().with(TypeValidation.of(config.isDebugMode()))
                                       .with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE);

        AgentBuilder agentBuilder = new AgentBuilder.Default(byteBuddy)
                                                    .with(poolStrategyCache);

        if (config.isAttachedInRuntime() || moduleDescription.isStoppable()) {
            LazyLogger.infoColor(() -> "Retransformation Strategy was activated.");
            agentBuilder = agentBuilder.disableClassFormatChanges()
                                       .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
        }

        return agentBuilder
                        .ignore(configuredMatcherList.apply(moduleDescription))
                        .or(any(), withTimeSpent(agentName(),"classloader", "bootstrap", isBootstrapClassLoader()))
                        .or(any(), withTimeSpent(agentName(),"classloader", "extension", isExtensionClassLoader()))
                        .or(any(), withTimeSpent(agentName(),"classloader", "reflection", isReflectionClassLoader()));
    }

    AgentBuilder build(AgentConfiguration config, AgentModuleDescription moduleDescription, Instrumentation instrumentation) {
            return typeTransformations.build().foldLeft(newAgentBuilder(config, moduleDescription, instrumentation), (agent, typeTransformation) -> {
                val transformers = new ArrayList<AgentBuilder.Transformer>();
                transformers.addAll(typeTransformation.getMixins().toJavaList());
                transformers.addAll(typeTransformation.getTransformations().toJavaList());
                return agent.type(typeTransformation.getElementMatcher().get())
                            .transform(new AgentBuilder.Transformer.Compound(transformers));
            });
    }

    private static Function1<AgentModuleDescription,ElementMatcher.Junction<NamedElement>> ignoredMatcherList() {
        return (moduleDescription) -> moduleDescription.getWithinPackage()
                .map(within -> not(nameMatches(within)))
                .getOrElse(not(nameMatches(
                                List.of(
                                "sun\\..*",
                                "com\\.sun\\..*",
                                "java\\..*",
                                "javax\\..*",
                                "org\\.aspectj.\\..*",
                                "com\\.newrelic.\\..*",
                                "org\\.groovy.\\..*",
                                "net\\.bytebuddy.\\..*",
                                "\\.asm.\\..*",
                                "kamon\\.agent\\..*",
                                "kamon\\.testkit\\..*",
                                "kamon\\.instrumentation\\..*",
                                "akka\\.testkit\\..*",
                                "org\\.scalatest\\..*",
                                "scala\\.(?!concurrent).*").mkString("|")
                        ))
                );
    }

    protected String agentName() {
        return getClass().getSimpleName();
    }
}
