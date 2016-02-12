package kamon.agent.transformer;

import kamon.agent.util.AgentUtil;
import utils.AgentApiUtils;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class KamonInstrumentationTransformer {

    public static void replaceKamonInstrumentation(Instrumentation instrumentation) throws IOException {
        JarFile bootstrapJar = new JarFile(AgentUtil.load("agent-bootstrap"));
        JarEntry jarEntry = bootstrapJar.getJarEntry("kamon/agent/api/instrumentation/KamonInstrumentation.class");
        byte[] bytes = AgentApiUtils.streamToByteArray(bootstrapJar.getInputStream(jarEntry));
        instrumentation.addTransformer(new ApiClassFileTransformer("kamon/agent/api/instrumentation/KamonInstrumentation", bytes), true);
    }

    private static final class ApiClassFileTransformer implements ClassFileTransformer {
        private final byte[] bytes;
        private final String className;

        ApiClassFileTransformer(String className, byte[] bytes) {
            this.className = className;
            this.bytes = bytes;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (this.className.equals(className)) {
                return this.bytes;
            }
            return null;
        }
    }

}
