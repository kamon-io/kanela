package kamon.agent;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.commons.RemappingMethodAdapter;
import net.bytebuddy.jar.asm.commons.SimpleRemapper;
import net.bytebuddy.jar.asm.tree.ClassNode;
import net.bytebuddy.jar.asm.tree.FieldNode;
import net.bytebuddy.jar.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static net.bytebuddy.jar.asm.Opcodes.ASM4;

public class KamonAgent {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentationLoader.class);


    private static void replaceKamonInstrumentation(Instrumentation instrProxy, JarFile bridgeJarFile) throws IOException {
        JarEntry jarEntry = bridgeJarFile.getJarEntry("kamon/agent/api/instrumentation/KamonInstrumentation.class");
        byte[] bytes = KamonAgent.read(bridgeJarFile.getInputStream(jarEntry), true);
        instrProxy.addTransformer(new ApiClassTransformer(bytes), true);
    }

    public static File load(String jarNameWithoutExtension) throws IOException {
        InputStream jarStream = ClassLoader.getSystemClassLoader().getResourceAsStream(jarNameWithoutExtension + ".jar");
        if (jarStream == null) {
            throw new FileNotFoundException(jarNameWithoutExtension + ".jar");
        }
        File file = File.createTempFile(jarNameWithoutExtension, ".jar");
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);
        try {
            KamonAgent.copy(jarStream, out, 8096, true);
            File file2 = file;
            return file2;
        }
        finally {
            out.close();
        }
    }


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
        replaceKamonInstrumentation(instrumentation,new JarFile(load("kamon-bootstrap")));
//        instrumentation.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
//            try {
//                 logger.debug("class name: " + className);
//                if (className.equals("kamon/agent/api/instrumentation/KamonInstrumentation")) {
//                    logger.info("Transform Kamon Instrumentation");
//                    ClassNode classNode = new ClassNode();
//                    final ClassReader classReader = new ClassReader("kamon.agent.api.impl.instrumentation.KamonInstrumentation");
//                    classReader.accept(classNode, 0);
//                    ClassWriter classWriter1 = new ClassWriter(0);
//                    final MergeAdapter mergeAdapter = new MergeAdapter(classWriter1, classNode);
//                    classReader.accept(mergeAdapter, 0);
//                    return classWriter1.toByteArray();


//                    ClassNode cn = new ClassNode();
//                    cr.accept(cn, 0);
//                    // To load thid class and its dependencies
//                    new kamon.agent.api.impl.instrumentation.KamonInstrumentation() { };




//                    //Class.forName("kamon.agent.api.impl.instrumentation.KamonInstrumentation");
//                    final ClassReader classReader = new ClassReader("kamon.agent.api.impl.instrumentation.KamonInstrumentation");
//                    final ClassWriter classWriter = new ClassWriter(classReader, 0);
//                    classReader.accept(classWriter, 0);
//                    return classWriter.toByteArray();
//                }
//            } catch (Exception exc) {
//                logger.error("Error al reescribir la clase: " + exc.getMessage());
//            }
//            return new byte[0];
//        });
        // InstrumentationLoader.load(args, instrumentation);
        logger.info("End Pre Main method");
//        withTimeSpent(InstrumentationLoader.load(instrumentation)) {
//            timeSpent ⇒
//            log.info(s"Premain startup complete in $timeSpent ms");
        }


    static int copy(InputStream input, OutputStream output, int bufferSize, boolean closeStreams) throws IOException {
        try {
            byte[] buffer = new byte[bufferSize];
            int count = 0;
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            int n2 = count;
            return n2;
        }
        finally {
            if (closeStreams) {
                input.close();
                output.close();
            }
        }
    }

    static byte[] read(InputStream input, boolean closeInputStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        KamonAgent.copy(input, outStream, input.available(), closeInputStream);
        return outStream.toByteArray();
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

    public static class MergeAdapter extends ClassVisitor {
        private ClassNode cn;
        private String cname;

        public MergeAdapter(ClassVisitor cv,
                            ClassNode cn) {
            super(ASM4, cv);
            this.cn = cn;
        }
        public void visit(int version, int access,
                          String name, String signature,
                          String superName, String[] interfaces) {
            super.visit(version, access, "kamon/agent/api/instrumentation/KamonInstrumentation",
                    signature, superName, interfaces);
            this.cname = name;
        }
//        public void visitEnd() {
//            for(Iterator it = cn.fields.iterator();
//                it.hasNext();) {
//                ((FieldNode) it.next()).accept(this);
//            }
//            for(Iterator it = cn.methods.iterator();
//                it.hasNext();) {
//                MethodNode mn = (MethodNode) it.next();
//                String[] exceptions =
//                        new String[mn.exceptions.size()];
//                mn.exceptions.toArray(exceptions);
//                MethodVisitor mv =
//                        cv.visitMethod(
//                                mn.access, mn.name, mn.desc,
//                                mn.signature, exceptions);
//                mn.instructions.resetLabels();
//                mn.accept(new RemappingMethodAdapter(
//                        mn.access, mn.desc, mv,
//                        new SimpleRemapper(cname, cn.name)));
//            }
//            super.visitEnd();
//        }
    }

    static final class ApiClassTransformer
            implements ClassFileTransformer {
        private final byte[] bytes;

        ApiClassTransformer(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//            if (!ClassTransformer.isValidClassName(className)) {
//                return null;
//            }
            if ("kamon/agent/api/instrumentation/KamonInstrumentation".equals(className)) {
                return this.bytes;
            }
            return null;
        }
    }

}

