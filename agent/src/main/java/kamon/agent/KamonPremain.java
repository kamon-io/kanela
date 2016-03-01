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

            CodeSource codeSource = KamonPremain.class.getProtectionDomain().getCodeSource();
            File kamonAgentJar = getKamonAgentJar(codeSource);
            Class<?> mainEntryPointClass;

            if (kamonAgentJar == null) {
                // this is ok, running integration test in IDE
                mainEntryPointClass = Class.forName("kamon.agent.KamonAgent", true, KamonPremain.class.getClassLoader());
            } else {
                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(kamonAgentJar));
                mainEntryPointClass = Class.forName("kamon.agent.KamonAgent", true, null);
            }

            Method premainMethod = mainEntryPointClass.getMethod("premain", String.class, Instrumentation.class);
            premainMethod.invoke(null, args, instrumentation);

        } catch (Throwable t) {
            // log error but don't re-throw which would prevent monitored app from starting
            System.err.println("Kamon Agent not started: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static File getKamonAgentJar(CodeSource codeSource) throws Exception {
        File codeSourceFile = new File(codeSource.getLocation().toURI());
        if (codeSourceFile.getName().endsWith(".jar")) {
            return codeSourceFile;
        }
        throw new IOException("Could not determine kamon-agent jar location");
    }
}
