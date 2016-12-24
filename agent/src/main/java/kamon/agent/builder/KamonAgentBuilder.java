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

import javaslang.collection.List;
import kamon.agent.AgentConfiguration;
import kamon.agent.api.instrumentation.TypeTransformation;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.matcher.ElementMatchers.nameMatches;

abstract class KamonAgentBuilder {

    List<TransformerDescription> transformersByTypes = List.empty();

    AgentBuilder build(AgentConfiguration config) {
        return transformersByTypes
                .foldLeft(newAgentBuilder(config), (agent, transformerByType) ->
                        agent.type(transformerByType.getElementMatcher())
                                .transform(transformerByType.getTransformer())
                                .asDecorator());
    }

    protected abstract AgentBuilder newAgentBuilder(AgentConfiguration config);

    public abstract void addTypeTransformation(TypeTransformation typeTransformation);

    AgentBuilder from(AgentConfiguration config) {
        val ignoreList = ignoredMatcherList(config);
        val byteBuddy = new ByteBuddy()
                .with(TypeValidation.of(config.isDebugMode()))
                .with(MethodGraph.Empty.INSTANCE);
        val agentBuilder = new AgentBuilder
                .Default(byteBuddy)
                .disableClassFormatChanges();
//                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)

        return ignoreList.foldLeft(agentBuilder, AgentBuilder::ignore)
                .ignore(any(), isBootstrapClassLoader());
//                .ignore(any(), isExtensionClassLoader());
    }

    List<ElementMatcher.Junction<NamedElement>> ignoredMatcherList(AgentConfiguration config) {
        return config.getWithinPackage()
                .map(within -> List.of(not(nameMatches(within))))
                .getOrElse(List.of(
                        nameMatches("sun\\..*"),
                        nameMatches("java\\..*"),
                        nameMatches("javax\\..*"),
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
}
