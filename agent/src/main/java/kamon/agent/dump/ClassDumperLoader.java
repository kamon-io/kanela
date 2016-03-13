package kamon.agent.dump;

import javaslang.control.Option;
import kamon.agent.KamonAgentConfig;

import java.lang.instrument.Instrumentation;

public class ClassDumperLoader {

    public static void load(Instrumentation instrumentation, KamonAgentConfig.DumpConfig config) {
        if (config.getDumpEnabled()) {
            if (config.getOnTheFly()) {
                new ClassDumperOnTheFly(config.getDumpDir(), config.getClassesPattern(), createOnFinishCallback(config)).install(instrumentation);
            } else {
                new ClassDumperOnShutdown(config.getDumpDir(), config.getClassesPattern(), createOnFinishCallback(config)).install(instrumentation);
            }
        }
    }

    private static Option<Runnable> createOnFinishCallback(KamonAgentConfig.DumpConfig config) {
        if (config.getCreateJar()) {
            return Option.some(() -> JarCreator.createJar(config.getJarName(), config.getDumpDir()));
        } else {
            return Option.none();
        }
    }
}
