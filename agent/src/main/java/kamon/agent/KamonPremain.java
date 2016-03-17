package kamon.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.jar.JarFile;

public class KamonPremain {

    public static void premain(String args, Instrumentation instrumentation) {
        try {

            final CodeSource codeSource = KamonPremain.class.getProtectionDomain().getCodeSource();
            final File kamonAgentJar = getKamonAgentJar(codeSource);

            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(kamonAgentJar));

            final Class<?> agentClass = Class.forName("kamon.agent.KamonAgent", true, null);
            final Method premainMethod = agentClass.getMethod("premain", String.class, Instrumentation.class);

            premainMethod.invoke(null, args, instrumentation);

        } catch (Throwable t) {
            // log error but don't re-throw which would prevent monitored app from starting
            System.err.println("Kamon Agent not started: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static File getKamonAgentJar(CodeSource codeSource) throws Exception {
        final File codeSourceFile = new File(codeSource.getLocation().toURI());
        if (codeSourceFile.getName().endsWith(".jar")) {
            return codeSourceFile;
        }
        throw new IOException("Could not determine kamon-agent jar location");
    }
}
