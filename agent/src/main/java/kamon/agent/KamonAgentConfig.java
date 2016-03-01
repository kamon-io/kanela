package kamon.agent;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import kamon.agent.util.log.LazyLogger;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class KamonAgentConfig {

    @Getter
    private List<String> instrumentations = new ArrayList<>();

    public KamonAgentConfig() {
        loadConfig();
    }

    private void loadConfig() {
        try {
            Config config = loadDefaultConfig().getConfig("kamon.agent");
            instrumentations = config.getStringList("instrumentations");
        } catch(ConfigException.Missing missing) {
            LazyLogger.warn(KamonAgentConfig.class, () -> "The instrumentations have not been found. Perhaps you have forgotten to add them to the config?", missing);
        }
    }

    private Config loadDefaultConfig() { return ConfigFactory.load("META-INF/reference");}
}
