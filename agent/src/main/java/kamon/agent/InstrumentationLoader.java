package kamon.agent;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import kamon.agent.api.instrumentation.KamonInstrumentation;
import kamon.agent.util.log.LazyLogger;

import java.lang.instrument.Instrumentation;

public class InstrumentationLoader {
    private static final Config factory = ConfigFactory.load();
    private static final LazyLogger logger = LazyLogger.create(InstrumentationLoader.class);

    public static void load(Instrumentation instrumentation) {
        Config config = factory.getConfig("kamon.agent");
        config.getStringList("instrumentations").forEach(clazz -> {
            try {
                ((KamonInstrumentation) Class.forName(clazz).newInstance()).register(instrumentation);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }
}

