package kamon.agent;

import kamon.agent.util.log.LazyLogger;

import java.lang.instrument.Instrumentation;

import static java.text.MessageFormat.format;
import static kamon.agent.util.AgentUtil.withTimeSpent;

public class KamonAgent {

    private static volatile LazyLogger log = LazyLogger.create(KamonAgent.class);

    /**
     * JVM hook to statically load the javaagent at startup.
     *
     * After the Java Virtual Machine (JVM) has initialized, the premain method
     * will be called. Then the real application main method will be called.
     *
     * @param args
     * @param instrumentation
     * @throws Exception
     */
    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        withTimeSpent(() -> {
            InstrumentationLoader.load(instrumentation, new KamonAgentConfig());
        }, (timeSpent) -> log.info(() -> format("Premain startup complete in {0} ms", timeSpent)));
    }


    /**
     * JVM hook to dynamically load javaagent at runtime.
     *
     * The agent class may have an agentmain method for use when the agent is
     * started after VM startup.
     *
     * @param args
     * @param instrumentation
     * @throws Exception
     */
    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        premain(args, instrumentation);
    }
}
