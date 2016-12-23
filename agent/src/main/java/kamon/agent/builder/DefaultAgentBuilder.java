package kamon.agent.builder;

import kamon.agent.KamonAgentConfig;
import kamon.agent.api.instrumentation.TypeTransformation;
import kamon.agent.api.instrumentation.listener.DebugInstrumentationListener;
import kamon.agent.api.instrumentation.listener.DefaultInstrumentationListener;
import kamon.agent.api.instrumentation.listener.dumper.ClassDumperListener;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.ArrayList;

@Value(staticConstructor = "instance")
@EqualsAndHashCode(callSuper = false)
class DefaultAgentBuilder extends KamonAgentBuilder {

    public AgentBuilder newAgentBuilder(KamonAgentConfig config) {
        return from(config)
                .with(DefaultInstrumentationListener.instance())
                .with(additionalListeners(config));
    }

    public void addTypeTransformation(TypeTransformation typeTransformation) {
        if (typeTransformation.isActive()) {
            transformersByTypes = transformersByTypes.appendAll(typeTransformation
                    .getTransformations()
                    .map(transformer -> TransformerDescription.of(extractElementMatcher(typeTransformation), transformer)));
        }
    }

    private AgentBuilder.Listener additionalListeners(KamonAgentConfig config) {
        val listeners = new ArrayList<AgentBuilder.Listener>();
        if (config.getDump().isDumpEnabled()) listeners.add(ClassDumperListener.instance(config.getDump()));
        if (config.getDebugMode()) listeners.add(DebugInstrumentationListener.instance());
        return new AgentBuilder.Listener.Compound(listeners);
    }
}
