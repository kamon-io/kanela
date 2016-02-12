package kamon.agent;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import kamon.agent.api.instrumentation.KamonInstrumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;

public class InstrumentationLoader {
    private static final Config factory = ConfigFactory.load();
    private static final Logger logger = LoggerFactory.getLogger(InstrumentationLoader.class);

    public static void load(String args, Instrumentation instrumentation) throws IOException, URISyntaxException {
        Config config = factory.getConfig("kamon.agent");
        config.getStringList("instrumentations").forEach(clazz -> {
            try {
                ((KamonInstrumentation) Class.forName(clazz).newInstance()).register(instrumentation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

