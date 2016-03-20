package kamon.agent.dump;

import kamon.agent.KamonAgent;
import kamon.agent.util.log.LazyLogger;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class ClassDumperTransformer implements ClassFileTransformer {

    private static final LazyLogger log = LazyLogger.create(ClassDumperTransformer.class);

    private ClassDumper classDumper;

    public ClassDumperTransformer(ClassDumper classDumper) {
        this.classDumper = classDumper;
    }

    public byte[] transform(ClassLoader loader, String className,
                            Class redefinedClass, ProtectionDomain protDomain,
                            byte[] classBytes) {
        try {

            // check and dump .class file
            if (className == null) return null;
            if(className.startsWith(KamonAgent.class.getPackage().getName().replace('.', '/'))) return null;
            if (classDumper.isCandidate(className)) {
                synchronized (this) {
                    classDumper.addClassToDump(className, classBytes);
                }
            }

        } catch (Exception exc) {
            //exc.printStackTrace();
            log.error(() -> "Failed on ClassDumperTransformer", exc);
        }

        // we don't mess with .class file, just return null
        return null;
    }
}
