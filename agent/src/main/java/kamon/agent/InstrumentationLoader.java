package kamon.agent;


import kamon.agent.api.instrumentation.KamonInstrumentation;
import kamon.agent.util.log.LazyLogger;

import java.lang.instrument.Instrumentation;

public class InstrumentationLoader {
    private static final LazyLogger log = LazyLogger.create(InstrumentationLoader.class);

    public static void load(Instrumentation instrumentation, KamonAgentConfig kamonAgentConfig) {
        kamonAgentConfig.getInstrumentations().forEach(clazz -> {
            try {
                ((KamonInstrumentation) Class.forName(clazz, false, ClassLoader.getSystemClassLoader()).newInstance()).register(instrumentation);
                log.info(() -> "Loaded " + clazz + "...");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }
}