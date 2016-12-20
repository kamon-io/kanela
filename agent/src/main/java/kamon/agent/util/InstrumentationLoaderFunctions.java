package kamon.agent.util;

import javaslang.Function1;
import javaslang.Function2;
import javaslang.Function4;
import javaslang.collection.List;
import javaslang.collection.Set;
import kamon.agent.api.instrumentation.KamonInstrumentation;
import kamon.agent.api.instrumentation.TypeTransformation;
import net.bytebuddy.agent.builder.AgentBuilder;
import lombok.val;

public class InstrumentationLoaderFunctions {

    /**
     *TODO
     */
    private static Function4<TypeTransformation, Function2<AgentBuilder, AgentBuilder.Transformer, AgentBuilder>, Function1<TypeTransformation, Set<AgentBuilder.Transformer>>, AgentBuilder, AgentBuilder> applyTransformation =
            (typeTransformation, addTransformerFunc, extractTransformers, agent) -> {
                if (typeTransformation.isActive()) return extractTransformers.apply(typeTransformation).foldLeft(agent, addTransformerFunc);
                else return agent;
            };
    /**
     * TODO
     */
    public static Function2<Agents, KamonInstrumentation, Agents> addTransformationsToBuilders =
            (agents, kamonInstrumentation) -> List.ofAll(kamonInstrumentation.collectTransformations()).foldLeft(agents, (agent, typeTransformation) -> {
                val transformationFunction = applyTransformation.curried().apply(typeTransformation).curried().apply(addTransformer(typeTransformation));
                val defaultBuilder = transformationFunction.apply(TypeTransformation::getTransformations).apply(agent.defaultAgent);
                val mixinsBuilder = transformationFunction.apply(TypeTransformation::getMixins).apply(agent.mixinsAgent);
                return Agents.of(defaultBuilder, mixinsBuilder);
            });

    /**
     *TODO
     */
    private static Function2<AgentBuilder, AgentBuilder.Transformer, AgentBuilder> addTransformer(TypeTransformation typeTransformation) {
        return (agent, transformer) ->
                agent.type(typeTransformation.getElementMatcher().getOrElseThrow(() -> new RuntimeException("There must be an element selected by elementMatcher")))
                        .transform(transformer)
                        .asDecorator();
    }
}
