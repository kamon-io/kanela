package kamon.agent.api.instrumentation.listener;

import kamon.agent.api.banner.AnsiColor;
import kamon.agent.util.log.LazyLogger;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import static java.text.MessageFormat.format;

@Value(staticConstructor = "instance")
@EqualsAndHashCode(callSuper = false)
public class DebugInstrumentationListener extends AgentBuilder.Listener.Adapter {

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {
        LazyLogger.info(() -> AnsiColor.ParseColors(format(":yellow,n:Transformed => {0} and loaded from {1}", typeDescription, classLoader)));
    }

    @Override
    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
        LazyLogger.debug(() -> AnsiColor.ParseColors(format(":red,n:Ignored => {0} and loaded from {1}", typeDescription, classLoader)));

    }
}
