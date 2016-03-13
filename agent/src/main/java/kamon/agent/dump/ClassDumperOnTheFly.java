package kamon.agent.dump;

import javaslang.control.Option;

import java.lang.instrument.Instrumentation;

public class ClassDumperOnTheFly extends ClassDumper {

    private Option<Runnable> onFinish = Option.none();

    public ClassDumperOnTheFly(String dumpDirArg, String classesRegexArg) {
        super(dumpDirArg, classesRegexArg);
    }

    public ClassDumperOnTheFly(String dumpDirArg, String classesRegexArg, Option<Runnable> onFinish) {
        this(dumpDirArg, classesRegexArg);
        this.onFinish = onFinish;
    }

    @Override
    protected void installTransformer(Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassDumperTransformer(this), true);
        onFinish.forEach(runnable -> Runtime.getRuntime().addShutdownHook(new Thread(runnable)));
    }

    @Override
    public void addClassToDump(String className, byte[] classBytes) {
        this.dumpClass(className, classBytes);
    }
}
