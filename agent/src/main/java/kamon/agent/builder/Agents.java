package kamon.agent.builder;

import javaslang.collection.List;
import kamon.agent.KamonAgentConfig;
import kamon.agent.api.instrumentation.TypeTransformation;
import lombok.Value;

import java.lang.instrument.Instrumentation;

@Value(staticConstructor = "from")
public class Agents {
    KamonAgentConfig config;
    List<KamonAgentBuilder> agentBuilders = List.of(MixinAgentBuilder.instance(), DefaultAgentBuilder.instance());

    public void install(Instrumentation instrumentation) {
        agentBuilders.forEach(builder -> builder.build(config).installOn(instrumentation));
    }

    public Agents addTypeTransformation(TypeTransformation typeTransformation) {
        agentBuilders.forEach(builder -> builder.addTypeTransformation(typeTransformation));
        return this;
    }
}
