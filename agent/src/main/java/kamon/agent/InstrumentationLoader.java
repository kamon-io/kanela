package kamon.agent;


import javaslang.Function1;
import javaslang.Function2;
import javaslang.collection.List;
import javaslang.collection.Set;
import kamon.agent.api.instrumentation.KamonInstrumentation;
import kamon.agent.api.instrumentation.TypeTransformation;
import kamon.agent.api.instrumentation.listener.InstrumentationListener;
import kamon.agent.util.log.LazyLogger;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static java.text.MessageFormat.format;

public class InstrumentationLoader {

    private final static InstrumentationListener instrumentationListener = new InstrumentationListener();

    public static void load(Instrumentation instrumentation, KamonAgentConfig kamonAgentConfig) {
        final AgentBuilder defaultAgent = createAgentBuilder(kamonAgentConfig);
        final AgentBuilder mixinsAgent = createAgentBuilder(kamonAgentConfig);
        final Agents agentBuilders = new Agents(defaultAgent, mixinsAgent);
        final List<String> instrumentations = List.ofAll(kamonAgentConfig.getInstrumentations());

        final Agents agents = instrumentations.foldLeft(agentBuilders, (agent, clazz) -> {
            try {
                final KamonInstrumentation kamonInstrumentation = (KamonInstrumentation) Class.forName(clazz, true, InstrumentationLoader.class.getClassLoader()).newInstance();
                return loadTransformations(agent, List.ofAll(kamonInstrumentation.collectTransformations()), clazz);
            } catch (Throwable e) {
                e.printStackTrace();
                return agent;

            }
        });
        agents.install(instrumentation);
    }

    private static Agents loadTransformations(Agents agentsPartial, List<TypeTransformation> typeTransformations, String clazz) {

        LazyLogger.info(() -> format("Loaded {0}...", clazz));

        return typeTransformations.foldLeft(agentsPartial, (agents, typeTransformation) -> {
            final Function2<AgentBuilder, AgentBuilder.Transformer, AgentBuilder> addTransformerFunc = addTransformerFunc(typeTransformation);
            return new Agents(
                    applyTransformation(typeTransformation, agents.defaultAgent, TypeTransformation::getTransformations, addTransformerFunc),
                    applyTransformation(typeTransformation, agents.mixinsAgent, TypeTransformation::getMixins, addTransformerFunc));
        });
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

    private static Function2<AgentBuilder, AgentBuilder.Transformer, AgentBuilder> addTransformerFunc(TypeTransformation typeTransformation) {
        return (agent, transformer) ->
                agent.type(typeTransformation.getElementMatcher().getOrElseThrow(() -> new RuntimeException("There must be an element selected by elementMatcher")))
                     .transform(transformer)
                     .asDecorator();
    }

    private static AgentBuilder createAgentBuilder(KamonAgentConfig config) {
        final List<ElementMatcher.Junction<NamedElement>> ignoreList = getIgnoredMatcherList(config);

        final AgentBuilder agentBuilder = new AgentBuilder.Default()
                .disableClassFormatChanges()
//                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(instrumentationListener);

        return ignoreList.foldLeft(agentBuilder, AgentBuilder::ignore)
                .ignore(any(), isBootstrapClassLoader());
//                .ignore(any(), isExtensionClassLoader());
    }

    private static List<ElementMatcher.Junction<NamedElement>> getIgnoredMatcherList(KamonAgentConfig config) {
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

    @Value
    private static class Agents {
        AgentBuilder defaultAgent;
        AgentBuilder mixinsAgent;

        void install(Instrumentation instrumentation) {
            mixinsAgent.installOn(instrumentation);
            defaultAgent.installOn(instrumentation);
        }
    }
}