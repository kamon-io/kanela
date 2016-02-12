package kamon.agent;

import kamon.agent.util.log.LazyLogger;

import java.lang.instrument.Instrumentation;

public class KamonAgent {

    private static final LazyLogger logger = LazyLogger.create(InstrumentationLoader.class);

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
        logger.info(() -> String.format("Start Pre Main method invoked with args: %s and inst: %s", args, instrumentation.toString()));
        InstrumentationLoader.load(instrumentation);
        logger.info(() -> "End Pre Main method");
//        withTimeSpent(InstrumentationLoader.load(instrumentation)) {
//            timeSpent ⇒
//            log.info(s"Premain startup complete in $timeSpent ms");
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
//    @throws(classOf[Exception])
    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        logger.debug(() -> String.format("agentmain method invoked with args: %s and inst: %s", args, instrumentation.toString()));
        premain(args, instrumentation);
    }
}

