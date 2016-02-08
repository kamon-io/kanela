package kamon.agent.api.instrumentation;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import kamon.api.instrumentation.KamonInstrumentation;

import java.lang.instrument.Instrumentation;

public class InstrumentationLoader {
    private static final Config factory = ConfigFactory.load();

    public static void load(Instrumentation instrumentation) {
        Config config = factory.getConfig("kamon.agent");
        config.getStringList("instrumentations").forEach(clazz -> {
            try {
                ((KamonInstrumentation) Class.forName(clazz).newInstance()).register(instrumentation);
            } catch (Exception e) {
                e.printStackTrace();
            }
//            log.info(s"$clazz registered.")
        });
    }
}

