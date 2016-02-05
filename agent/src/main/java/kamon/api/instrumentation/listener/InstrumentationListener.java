package kamon.api.instrumentation.listener;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;

public class InstrumentationListener implements AgentBuilder.Listener {
    @Override
    public void onTransformation(TypeDescription typeDescription, DynamicType dynamicType) {

    }

    @Override
    public void onIgnored(TypeDescription typeDescription) {

    }

    @Override
    public void onError(String s, Throwable throwable) {

    }

    @Override
    public void onComplete(String s) {

    }
}
