package kamon.agent;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import javaslang.control.Option;
import javaslang.control.Try;
import kamon.agent.util.log.LazyLogger;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class KamonAgentConfig {

    private static final LazyLogger log = LazyLogger.create(KamonAgentConfig.class);

    @Getter
    private List<String> instrumentations = new ArrayList<>();
    @Getter
    private DumpConfig dump;

    public KamonAgentConfig() {
        loadConfig();
    }

    private void loadConfig() {
        try {
            Config config = loadDefaultConfig().getConfig("kamon.agent");
            Try.run(() -> instrumentations = config.getStringList("instrumentations"))
                    .onFailure(exc -> log.warn(
                            () -> "The instrumentations have not been found. Perhaps you have forgotten to add them to the config?", exc));
            Option<Boolean> dumpEnabled = Try.of(() -> Option.some(config.getBoolean("dump.enabled")) ).getOrElse(Option.none());
            Option<String> dumpDir = Try.of(() -> Option.some(config.getString("dump.dir")) ).getOrElse(Option.none());
            Option<String> classesPattern = Try.of(() -> Option.some(config.getString("dump.classes")) ).getOrElse(Option.none());
            Option<Boolean> onTheFly = Try.of(() -> Option.some(config.getBoolean("dump.on-the-fly")) ).getOrElse(Option.none());
            Option<Boolean> createJar = Try.of(() -> Option.some(config.getBoolean("dump.create-jar")) ).getOrElse(Option.none());
            Option<String> jarName = Try.of(() -> Option.some(config.getString("dump.jar-name")) ).getOrElse(Option.none());
            this.dump = new DumpConfig(dumpEnabled, dumpDir, classesPattern, onTheFly, createJar, jarName);
        } catch(ConfigException.Missing missing) {
            log.warn(() -> "It has not been found any configuration for Kamon Agent.", missing);
        }
    }

    private Config loadDefaultConfig() { return ConfigFactory.load("META-INF/reference");}

    public class DumpConfig {
        @Getter
        private Boolean dumpEnabled = false;
        @Getter
        private String dumpDir = "./target/classes-dump";
        @Getter
        private String classesPattern = ".*";
        @Getter
        private Boolean onTheFly = false;
        @Getter
        private Boolean createJar = true;
        @Getter
        private String jarName = "instrumentedClasses";

        public DumpConfig(Option<Boolean> dumpEnabled,
                          Option<String> dumpDir,
                          Option<String> classesPattern,
                          Option<Boolean> onTheFly,
                          Option<Boolean> createJar,
                          Option<String> jarName) {
            dumpEnabled.forEach((enabled) -> this.dumpEnabled = enabled);
            dumpDir.forEach((dir) -> this.dumpDir = dir);
            classesPattern.forEach((regex) -> this.classesPattern = regex);
            onTheFly.forEach((fly) -> this.onTheFly = fly);
            createJar.forEach((jar) -> this.createJar = jar);
            jarName.forEach((name) -> this.jarName = name);
        }
    }
}
