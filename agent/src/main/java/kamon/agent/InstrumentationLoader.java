package kamon.agent;

import javaslang.Function1;
import javaslang.Function2;
import javaslang.collection.List;
import javaslang.collection.Set;
import javaslang.control.Try;
import kamon.agent.api.instrumentation.KamonInstrumentation;
import kamon.agent.api.instrumentation.TypeTransformation;
import kamon.agent.util.Agents;
import kamon.agent.util.log.LazyLogger;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

import static java.text.MessageFormat.format;

public class InstrumentationLoader {

    public static void load(Instrumentation instrumentation) {
        final KamonAgentConfig kamonAgentConfig = KamonAgentConfig.instance();
        final AgentBuilder defaultAgent = Agents.builderFrom(kamonAgentConfig);
        final AgentBuilder mixinsAgent = Agents.builderFrom(kamonAgentConfig);
        final Agents agentBuilders = Agents.of(defaultAgent, mixinsAgent);

        kamonAgentConfig.getInstrumentations()
                        .map(InstrumentationLoader::loadInstrumentation)
                        .sortBy(KamonInstrumentation::order)
                        .foldLeft(agentBuilders, addTransformationsToBuilders())
                        .install(instrumentation);
    }

    private static Function2<Agents, KamonInstrumentation, Agents> addTransformationsToBuilders() {
        return (agents, kamonInstrumentation) -> List.ofAll(kamonInstrumentation.collectTransformations()).foldLeft(agents, (agent, typeTransformation) -> {
            final AgentBuilder defaultBuilder = applyTransformation(typeTransformation, agent.defaultAgent, TypeTransformation::getTransformations, addTransformer(typeTransformation));
            final AgentBuilder mixinsBuilder = applyTransformation(typeTransformation, agent.mixinsAgent, TypeTransformation::getMixins, addTransformer(typeTransformation));
            return Agents.of(defaultBuilder, mixinsBuilder);
        });
    }

    private static KamonInstrumentation loadInstrumentation(String instrumentationClass) {
        LazyLogger.info(() -> format("Loaded {0}...", instrumentationClass));
        return Try.of(() -> (KamonInstrumentation) Class.forName(instrumentationClass, true, InstrumentationLoader.class.getClassLoader()).newInstance())
                  .recover(KamonInstrumentation.NoOp::new)
                  .get();
    }

    private static AgentBuilder applyTransformation(TypeTransformation typeTransformation,
                                                    AgentBuilder agent,
                                                    Function1<TypeTransformation, Set<AgentBuilder.Transformer>> extractTransformers,
                                                    Function2<AgentBuilder, AgentBuilder.Transformer, AgentBuilder> addTransformerFunc) {
        if (typeTransformation.isActive())
            return extractTransformers.apply(typeTransformation).foldLeft(agent, addTransformerFunc);
        else
            return agent;
    }

    private static Function2<AgentBuilder, AgentBuilder.Transformer, AgentBuilder> addTransformer(TypeTransformation typeTransformation) {
        return (agent, transformer) ->
                agent.type(typeTransformation.getElementMatcher().getOrElseThrow(() -> new RuntimeException("There must be an element selected by elementMatcher")))
                     .transform(transformer)
                     .asDecorator();
    }
}