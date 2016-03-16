package kamon.agent;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import kamon.agent.util.log.LazyLogger;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class KamonAgentConfig {

    private static final LazyLogger log = LazyLogger.create(KamonAgentConfig.class);

    @Getter
    private List<String> instrumentations = new ArrayList<>();
    @Getter
    private Boolean dumpEnabled = false;
    @Getter
    private String dumpDir = "./target/classes-dump";
    @Getter
    private String classesPattern = "app.kamon.instrumentation.Pepe.*";

    public KamonAgentConfig() {
        loadConfig();
    }

    private void loadConfig() {
        try {
            Config config = loadDefaultConfig().getConfig("kamon.agent");
            tryLoad(() -> instrumentations = config.getStringList("instrumentations"),
                    (ex) -> log.warn(() -> "The instrumentations have not been found. Perhaps you have forgotten to add them to the config?", ex));
            tryLoad(() -> dumpEnabled = config.getBoolean("dump.enabled"));
            tryLoad(() -> dumpDir = config.getString("dump.dir"));
            tryLoad(() -> classesPattern = config.getString("dump.classes"));
        } catch(ConfigException.Missing missing) {
            log.warn(() -> "It has not been found any configuration for Kamon Agent.", missing);
        }
    }

    private void tryLoad(Runnable runnable) {
        tryLoad(runnable, (ex) -> {});
    }

    private void tryLoad(Runnable runnable, Consumer<? super Exception> recoveryOnError) {
        try {
            runnable.run();
        } catch (Exception ex) {
            recoveryOnError.accept(ex);
        }
    }

    private Config loadDefaultConfig() { return ConfigFactory.load("META-INF/reference");}
}
