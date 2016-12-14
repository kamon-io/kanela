package kamon.agent.api.instrumentation.listener;

import kamon.agent.api.banner.AnsiColor;
import kamon.agent.util.log.LazyLogger;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.io.IOException;

import static java.text.MessageFormat.format;

@Value(staticConstructor = "instance")
@EqualsAndHashCode(callSuper = false)
public class DebugInstrumentationListener extends AgentBuilder.Listener.Adapter {

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {
//        try {
//            dynamicType.saveIn(new File("/home/diego/dump"));
//            } catch (IOException e) {
//            e.printStackTrace();
//        }
        LazyLogger.info(() -> AnsiColor.ParseColors(format(":yellow,n:Transformed => {0} and loaded from {1}", typeDescription, classLoader)));
    }

    @Override
    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
//        LazyLogger.debug(() -> AnsiColor.ParseColors(format(":red,n:Ignored => {0} and loaded from {1}", typeDescription, classLoader)));

    }

    @Override
    public void onComplete(String typeName, ClassLoader classLoader, JavaModule module) {
//        LazyLogger.debug(() -> AnsiColor.ParseColors(format(":green,n: onComplete: {0}", s)));
    }
}
