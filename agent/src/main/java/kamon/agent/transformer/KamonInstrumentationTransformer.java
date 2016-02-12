package kamon.agent.transformer;

import kamon.agent.util.Util;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class KamonInstrumentationTransformer {

    public static void replaceKamonInstrumentation(Instrumentation instrumentation) throws IOException {
        JarFile bootstrapJar = new JarFile(Util.load("agent-bootstrap"));
        JarEntry jarEntry = bootstrapJar.getJarEntry("kamon/agent/api/instrumentation/KamonInstrumentation.class");
        byte[] bytes = Util.streamToByteArray(bootstrapJar.getInputStream(jarEntry));
        instrumentation.addTransformer(new KamonInstrumentationFileTransformer(bytes), true);
    }

    static final class KamonInstrumentationFileTransformer implements ClassFileTransformer {
        private final byte[] bytes;

        KamonInstrumentationFileTransformer(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if ("kamon/agent/api/instrumentation/KamonInstrumentation".equals(className)) {
                return this.bytes;
            }
            return null;
        }
    }

}
