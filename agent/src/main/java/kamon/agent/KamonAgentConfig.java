package kamon.agent;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import kamon.agent.util.log.LazyLogger;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cristian on 18/02/16.
 */
public class KamonAgentConfig {
    //private static final Config factory = ConfigFactory.load();
    private static final LazyLogger log = LazyLogger.create(InstrumentationLoader.class);

    @Getter
    private List<String> instrumentations = new ArrayList<>();

    public KamonAgentConfig() {
        loadConfig();
    }

    private void loadConfig() {
        try {
            Config config = loadDefaultConfig().getConfig("kamon.agent");
            instrumentations = config.getStringList("instrumentations");
        } catch(ConfigException.Missing exc) {
            log.warn(() -> "The instrumentations have not been found. Perhaps you have forgotten to add them to the config?", exc);
        } catch(Exception exc) {
            log.error(() -> "There was an error while trying to load the Kamon Agent.", exc);
        }
    }

    private Config loadDefaultConfig() {
        final Config defaultConfiguration = ConfigFactory.load("META-INF/reference");
//        final Config reference = ConfigFactory.load( "META-INF/reference" ).withFallback( defaultConfiguration );
//        return ConfigFactory.load( "conf/application" ).withFallback( reference );
        return defaultConfiguration;
    }
}
