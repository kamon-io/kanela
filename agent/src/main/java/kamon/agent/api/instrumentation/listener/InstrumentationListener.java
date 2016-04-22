package kamon.agent.api.instrumentation.listener;

import kamon.agent.api.banner.AnsiColor;
import kamon.agent.util.log.LazyLogger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;

import static java.text.MessageFormat.format;

public class InstrumentationListener implements AgentBuilder.Listener {

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, DynamicType dynamicType) {
        LazyLogger.info(() -> AnsiColor.ParseColors(format(":yellow,n:Transformed => {0} and loaded from {1}", typeDescription, classLoader)));
    }

    @Override
    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader) {
//        log.info(() -> typeDescription.toString());
    }

    @Override
    public void onError(String error, ClassLoader classLoader, Throwable cause) {
        LazyLogger.info(() -> AnsiColor.ParseColors(format(":red,n:Error => {0} with message {1}", error, cause.getMessage())));
    }

    @Override
    public void onComplete(String s, ClassLoader classLoader) {
//        log.info(() -> s);
    }
}
