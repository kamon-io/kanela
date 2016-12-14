package kamon.agent.api.instrumentation.listener;

import kamon.agent.api.banner.AnsiColor;
import kamon.agent.util.log.LazyLogger;
import lombok.EqualsAndHashCode;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Listener;
import net.bytebuddy.utility.JavaModule;

import static java.text.MessageFormat.format;
import lombok.Value;

@Value(staticConstructor = "instance")
@EqualsAndHashCode(callSuper = false)
public class DefaultInstrumentationListener extends Listener.Adapter {

    @Override
    public void onError(String error, ClassLoader classLoader, JavaModule module, Throwable throwable) {
        LazyLogger.info(() -> AnsiColor.ParseColors(format(":red,n:Error => {0} with message {1}. Class loader: {2}", error, throwable.getMessage(), classLoader)));
    }
}
