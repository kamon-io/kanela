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

package kamon.agent.builder;

import javaslang.Function1;
import javaslang.collection.List;
import kamon.agent.api.instrumentation.TypeTransformation;
import kamon.agent.cache.PoolStrategyCache;
import kamon.agent.util.ListBuilder;
import kamon.agent.util.conf.AgentConfiguration;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;

import static kamon.agent.util.matcher.ClassLoaderMatcher.isReflectionClassLoader;
import static kamon.agent.util.matcher.TimedMatcher.withTimeSpent;
import static net.bytebuddy.matcher.ElementMatchers.*;

abstract class KamonAgentBuilder {

    private static final Function1<AgentConfiguration, List<ElementMatcher.Junction<NamedElement>>> configuredMatcherList = ignoredMatcherList().memoized();
    private final PoolStrategyCache poolStrategyCache = PoolStrategyCache.instance();

    final ListBuilder<TransformerDescription> transformersByTypes = ListBuilder.builder();

    protected abstract AgentBuilder newAgentBuilder(AgentConfiguration config);
    protected abstract void addTypeTransformation(TypeTransformation typeTransformation);

    AgentBuilder from(AgentConfiguration config) {
        val byteBuddy = new ByteBuddy()
                .with(TypeValidation.of(config.isDebugMode()))
                .with(MethodGraph.Empty.INSTANCE);


        AgentBuilder agentBuilder = new AgentBuilder.Default(byteBuddy);
//                .with(poolStrategyCache);

        if (config.isAttachedInRuntime()) {
            agentBuilder.disableClassFormatChanges()
                        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
        }

        return configuredMatcherList.apply(config)
                                    .foldLeft(agentBuilder, AgentBuilder::ignore)
                                    .ignore(any(), withTimeSpent(getAgentName(),"classloader", "bootstrap", isBootstrapClassLoader()))
                                    .or(any(), withTimeSpent(getAgentName(),"classloader", "extension", isExtensionClassLoader()))
                                    .or(any(), withTimeSpent(getAgentName(),"classloader", "reflection", isReflectionClassLoader()));
    }

    AgentBuilder build(AgentConfiguration config) {
        return transformersByTypes.build()
                .foldLeft(newAgentBuilder(config), (agent, transformerByType) ->
                        agent.type(transformerByType.getElementMatcher())
                             .transform(transformerByType.getTransformer())
                             .asDecorator());
    }

    private static Function1<AgentConfiguration,List<ElementMatcher.Junction<NamedElement>>> ignoredMatcherList() {
        return (configuration) -> configuration.getWithinPackage()
                .map(within -> List.of(not(nameMatches(within))))
                .getOrElse(List.of(
                        nameMatches("sun\\..*"),
                        nameMatches("com\\.sun\\..*"),
                        nameMatches("java\\..*"),
                        nameMatches("javax\\..*"),
                        nameMatches("org\\.aspectj.\\..*"),
                        nameMatches("com\\.newrelic.\\..*"),
                        nameMatches("org\\.groovy.\\..*"),
                        nameMatches("net\\.bytebuddy.\\..*"),
                        nameMatches("\\.asm.\\..*"),
                        nameMatches("kamon\\.agent\\..*"),
                        nameMatches("kamon\\.testkit\\..*"),
                        nameMatches("kamon\\.instrumentation\\..*"),
                        nameMatches("akka\\.testkit\\..*"),
                        nameMatches("org\\.scalatest\\..*"),
                        nameMatches("scala\\.(?!concurrent).*")
                ));
    }

    ElementMatcher<? super TypeDescription> extractElementMatcher(TypeTransformation typeTransformation) {
        return typeTransformation.getElementMatcher().getOrElseThrow(() -> new RuntimeException("There must be an element selected by elementMatcher"));
    }

    protected String getAgentName() {
        return getClass().getSimpleName();
    }
}
