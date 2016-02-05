package kamon.agent;

import java.lang.instrument.Instrumentation;

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
//    @throws(classOf[Exception])
    public static void premain(String args,  Instrumentation instrumentation){
        InstrumentationLoader.load(instrumentation);
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
    public static void agentmain(String args, Instrumentation instrumentation) {
//        log.debug(s"agentmain method invoked with args: $args and inst: $instrumentation")
        premain(args, instrumentation);
    }
}