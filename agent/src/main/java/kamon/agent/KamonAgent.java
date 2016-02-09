package kamon.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.Arrays;

public class KamonAgent {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentationLoader.class);

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
    public static void premain(String args,  Instrumentation instrumentation) throws IOException, URISyntaxException {
        logger.info(String.format("Start Pre Main method invoked with args: %s and inst: %s", args, instrumentation.toString()));
        InstrumentationLoader.load(args, instrumentation);
        logger.info("End Pre Main method");
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
    public static void agentmain(String args, Instrumentation instrumentation) throws IOException, URISyntaxException {
        logger.debug(String.format("agentmain method invoked with args: %s and inst: %s", args, instrumentation.toString()));
        premain(args, instrumentation);
    }
}