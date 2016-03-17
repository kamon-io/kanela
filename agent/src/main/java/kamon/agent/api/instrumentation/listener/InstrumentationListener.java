package kamon.agent.api.instrumentation.listener;

import kamon.agent.api.banner.AnsiColor;
import kamon.agent.util.log.LazyLogger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;

import java.text.MessageFormat;

import static java.text.MessageFormat.format;

public class InstrumentationListener implements AgentBuilder.Listener {

    private static final LazyLogger log = LazyLogger.create(InstrumentationListener.class);

    @Override
    public void onTransformation(TypeDescription typeDescription, DynamicType dynamicType) {
        log.info(() -> AnsiColor.ParseColors(format(":yellow,n: Transformed => {0}", typeDescription)));
    }

    @Override
    public void onIgnored(TypeDescription typeDescription) {
//        log.info(() -> typeDescription.toString());
    }

    @Override
    public void onError(String error, Throwable cause) {
        log.info(() -> AnsiColor.ParseColors(format(":red,n: Error => {0} with message {1}", error, cause.getMessage())));
    }

    @Override
    public void onComplete(String s) {
//        log.info(() -> s);
    }
}
