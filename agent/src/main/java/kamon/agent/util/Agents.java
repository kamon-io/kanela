package kamon.agent.util;

import javaslang.collection.List;
import kamon.agent.KamonAgentConfig;
import kamon.agent.api.instrumentation.listener.DebugInstrumentationListener;
import kamon.agent.api.instrumentation.listener.DefaultInstrumentationListener;
import kamon.agent.api.instrumentation.listener.dumper.ClassDumperListener;
import lombok.Value;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;

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

    public static Agents from(KamonAgentConfig config){
        val defaultAgent = build(config).with(DefaultInstrumentationListener.instance()).with(additionalListeners(config));
        val mixinsAgent = build(config).with(DefaultInstrumentationListener.instance());
        return new Agents(defaultAgent, mixinsAgent);
    }

    private static AgentBuilder build(KamonAgentConfig config) {
        val ignoreList = ignoredMatcherList(config);
        val byteBuddy = new ByteBuddy()
                .with(TypeValidation.of(config.isDebugMode()))
                .with(MethodGraph.Empty.INSTANCE);
        val agentBuilder = new AgentBuilder.Default(byteBuddy)
                .disableClassFormatChanges();
//                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//                .with(listeners(config));

        return ignoreList.foldLeft(agentBuilder, AgentBuilder::ignore)
                         .ignore(any(), isBootstrapClassLoader());
//                .ignore(any(), isExtensionClassLoader());
    }

    private static AgentBuilder.Listener additionalListeners(KamonAgentConfig config) {
        val listeners = new ArrayList<AgentBuilder.Listener>();
        if(config.getDump().isDumpEnabled()) listeners.add(ClassDumperListener.instance(config.getDump()));
        if(config.getDebugMode()) listeners.add(DebugInstrumentationListener.instance());
        return new AgentBuilder.Listener.Compound(listeners);
    }

    private static List<ElementMatcher.Junction<NamedElement>> ignoredMatcherList(KamonAgentConfig config) {
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