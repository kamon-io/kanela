package kamon.agent;

import kamon.agent.api.banner.KamonAgentBanner;

import java.lang.instrument.Instrumentation;

import static kamon.agent.util.AgentUtil.withTimeLogging;

public class KamonAgent {

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
        withTimeLogging(() -> {
            KamonAgentBanner.printBanner(System.out);
            InstrumentationLoader.load(instrumentation, new KamonAgentConfig());
        }, "Premain startup complete in");
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
