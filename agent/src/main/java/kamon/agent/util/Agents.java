package kamon.agent.util;

import javaslang.collection.List;
import kamon.agent.KamonAgentConfig;
import kamon.agent.api.instrumentation.listener.DebugInstrumentationListener;
import kamon.agent.api.instrumentation.listener.DefaultInstrumentationListener;
import kamon.agent.api.instrumentation.listener.dumper.ClassDumperListener;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.matcher.ElementMatcher;
import lombok.Value;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;

import static net.bytebuddy.matcher.ElementMatchers.*;

@Value(staticConstructor = "of")
public class Agents {
    public AgentBuilder defaultAgent;
    public AgentBuilder mixinsAgent;

    public void install(Instrumentation instrumentation) {
        mixinsAgent.installOn(instrumentation);
        defaultAgent.installOn(instrumentation);
    }

    public static AgentBuilder builderFrom(KamonAgentConfig config) {
        final List<ElementMatcher.Junction<NamedElement>> ignoreList = getIgnoredMatcherList(config);
        final AgentBuilder agentBuilder = new AgentBuilder.Default()
                .disableClassFormatChanges()
//                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(listeners(config));

        return ignoreList.foldLeft(agentBuilder, AgentBuilder::ignore)
                         .ignore(any(), isBootstrapClassLoader());
//                .ignore(any(), isExtensionClassLoader());
    }

    private static AgentBuilder.Listener listeners(KamonAgentConfig config) {
        final java.util.List<AgentBuilder.Listener> listeners = new ArrayList<>();
        if(config.getDump().getDumpEnabled()) listeners.add(ClassDumperListener.instance(config.getDump()));
        if(config.getIsDebugMode()) listeners.add(DebugInstrumentationListener.instance());
        listeners.add(DefaultInstrumentationListener.instance());
        return new AgentBuilder.Listener.Compound(listeners);
    }

    private static List<ElementMatcher.Junction<NamedElement>> getIgnoredMatcherList(KamonAgentConfig config) {
        return config.getWithinPackage()
                .map(within -> List.of(not(nameMatches(within))))
                .getOrElse(List.of(
                        nameMatches("sun\\..*"),
                        nameMatches("java\\..*"),
                        nameMatches("javax\\..*"),
                        nameMatches("kamon\\.agent\\..*"),
                        nameMatches("kamon\\.testkit\\..*"),
                        nameMatches("kamon\\.instrumentation\\..*"),
                        nameMatches("akka\\.testkit\\..*"),
                        nameMatches("org\\.scalatest\\..*"),
                        nameMatches("scala\\.(?!concurrent).*")
                ));
    }
}