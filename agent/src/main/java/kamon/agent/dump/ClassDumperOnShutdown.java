package kamon.agent.dump;

import javaslang.control.Option;
import kamon.agent.util.log.LazyLogger;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class ClassDumperOnShutdown extends ClassDumper {

    private static final LazyLogger log = LazyLogger.create(ClassDumperOnShutdown.class);

    private Option<Runnable> onFinish = Option.none();

    /**
     * It is important that this be a <i>linked</i> hash map because we need to generate hash numbers
     * for the classes in the order in which they are loaded. This is because a generated class <i>a</i> may reference
     * other generated classes, and when determining a hash code for <i>a</i>, the hash code for those
     * referenced classes must already have been computed.
     */
    protected final LinkedHashMap<String,byte[]> classNameToBytes = new LinkedHashMap<String, byte[]>();

    public ClassDumperOnShutdown(String dumpDirArg, String classesRegexArg) {
        super(dumpDirArg, classesRegexArg);
    }

    public ClassDumperOnShutdown(String dumpDirArg, String classesRegexArg, Option<Runnable> onFinish) {
        this(dumpDirArg, classesRegexArg);
        this.onFinish = onFinish;
    }

    @Override
    protected void installTransformer(Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassDumperTransformer(this), true);
        onShutdown();
    };

    @Override
    public void addClassToDump(String className, byte[] classBytes) {
        byte[] oldBytes = this.classNameToBytes.put(className, classBytes);
        if(oldBytes != null && !Arrays.equals(classBytes, oldBytes)) {
            log.warn(() -> "WARNING: There exist two different classes with name " + className);
        }
    };

    private void onShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    dumpClassesToDisk(classNameToBytes);
                    onFinish.forEach(Runnable::run);
                } catch (Exception exc) {
                    log.error(() -> "The class dumping on shutdown failed", exc);
                }
            }

        });
    }

    private synchronized void dumpClassesToDisk(LinkedHashMap<String, byte[]> classNameToBytes) {
        classNameToBytes.forEach(this::dumpClass);
    }
}
