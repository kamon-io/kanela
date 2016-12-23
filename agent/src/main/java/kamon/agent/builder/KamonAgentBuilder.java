package kamon.agent.builder;

import javaslang.collection.List;
import kamon.agent.KamonAgentConfig;
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

    AgentBuilder build(KamonAgentConfig config) {
        return transformersByTypes
                .foldLeft(newAgentBuilder(config), (agent, transformerByType) ->
                        agent.type(transformerByType.getElementMatcher())
                                .transform(transformerByType.getTransformer())
                                .asDecorator());
    }

    protected abstract AgentBuilder newAgentBuilder(KamonAgentConfig config);

    public abstract void addTypeTransformation(TypeTransformation typeTransformation);

    AgentBuilder from(KamonAgentConfig config) {
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

    List<ElementMatcher.Junction<NamedElement>> ignoredMatcherList(KamonAgentConfig config) {
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
