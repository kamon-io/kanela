package kamon.agent.api.instrumentation.listener;

import kamon.agent.api.banner.AnsiColor;
import kamon.agent.util.log.LazyLogger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;

public class InstrumentationListener implements AgentBuilder.Listener {

    private static final LazyLogger log = LazyLogger.create(InstrumentationListener.class);

    @Override
    public void onTransformation(TypeDescription typeDescription, DynamicType dynamicType) {
        log.info(() -> AnsiColor.ParseColors(":yellow,n: onTransformation: "  + typeDescription.toString()));
    }

    @Override
    public void onIgnored(TypeDescription typeDescription) {
//        log.info(() -> typeDescription.toString());
    }

    @Override
    public void onError(String s, Throwable throwable) {
        log.info(() -> AnsiColor.ParseColors(":red,n: onError: "  + s + ". ||| " + throwable.getMessage()));
    }

    @Override
    public void onComplete(String s) {
//        log.info(() -> s);
    }
}
