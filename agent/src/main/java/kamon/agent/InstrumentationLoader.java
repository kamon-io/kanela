package kamon.agent;


import javaslang.control.Option;
import kamon.agent.api.instrumentation.KamonInstrumentation;
import kamon.agent.util.ClassDumper;
import kamon.agent.util.log.LazyLogger;

import java.lang.instrument.Instrumentation;

public class InstrumentationLoader {
    public static void load(Instrumentation instrumentation, KamonAgentConfig kamonAgentConfig) {
        kamonAgentConfig.getInstrumentations().forEach(clazz -> {
            try {
                ((KamonInstrumentation) Class.forName(clazz, true, ClassLoader.getSystemClassLoader()).newInstance()).register(instrumentation);
                LazyLogger.info(InstrumentationLoader.class, () -> "Loaded " + clazz + "...");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        ClassDumper.process(instrumentation, Option.none(), Option.some("app.kamon.instrumentation.Pepe.*"));
    }
}

