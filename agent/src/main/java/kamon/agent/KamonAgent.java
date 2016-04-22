package kamon.agent;

import kamon.agent.api.banner.KamonAgentBanner;
import kamon.agent.dump.ClassDumperLoader;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

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
            final KamonAgentConfig kamonAgentConfig = new KamonAgentConfig();
//            instrumentation.addTransformer(new DebugClassloaderTransformer());
            InstrumentationLoader.load(instrumentation, kamonAgentConfig);
            ClassDumperLoader.load(instrumentation, kamonAgentConfig.getDump());
        }, "Premain startup complete in");
    }


    private static class DebugClassloaderTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

//            if("akka.dispatch.Envelope".contains(className)) {
                System.out.println("className:" + className + " from classloader: " + loader);
//            }
            return null;
        }
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
