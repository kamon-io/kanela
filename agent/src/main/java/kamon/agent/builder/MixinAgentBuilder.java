package kamon.agent.builder;

import kamon.agent.KamonAgentConfig;
import kamon.agent.api.instrumentation.TypeTransformation;
import kamon.agent.api.instrumentation.listener.DefaultInstrumentationListener;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;

@Value(staticConstructor = "instance")
@EqualsAndHashCode(callSuper = false)
class MixinAgentBuilder extends KamonAgentBuilder {

    public AgentBuilder newAgentBuilder(KamonAgentConfig config) {
        return from(config)
                .with(DefaultInstrumentationListener.instance());
    }

    public void addTypeTransformation(TypeTransformation typeTransformation) {
        if (typeTransformation.isActive()) {
            transformersByTypes = transformersByTypes.appendAll(typeTransformation
                    .getMixins()
                    .map(transformer -> TransformerDescription.of(extractElementMatcher(typeTransformation), transformer)));
        }
    }
}
