package kamon.agent;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import javaslang.collection.List;
import javaslang.collection.List.Nil;
import javaslang.control.Option;
import javaslang.control.Try;
import kamon.agent.util.log.LazyLogger;
import lombok.Value;

@Value
public class KamonAgentConfig {
    List<String> instrumentations;
    Option<String> withinPackage;
    Boolean debugMode;
    DumpConfig dump;

    private static class Holder {
        private static final KamonAgentConfig Instance = new KamonAgentConfig();
    }

    public static KamonAgentConfig instance() {
        return Holder.Instance;
    }

    private KamonAgentConfig() {
        Config config = getConfig();
        this.instrumentations = getInstrumentations(config);
        this.withinPackage = getWithinConfiguration(config);
        this.debugMode = getDebugMode(config);
        this.dump = new DumpConfig(config);
    }

    @Value
    public class DumpConfig {
        Boolean dumpEnabled;
        String dumpDir;
        Boolean onTheFly;
        Boolean createJar;
        String jarName;

        DumpConfig(Config config) {
            this.dumpEnabled = Try.of(() -> config.getBoolean("class-dumper.enabled")).getOrElse(false);
            this.dumpDir = Try.of(() -> config.getString("class-dumper.dir")).getOrElse( System.getProperty("user.home") + "/kamon-agent/dump");
            this.onTheFly = Try.of(() -> config.getBoolean("class-dumper.on-the-fly")).getOrElse(false);
            this.createJar = Try.of(() -> config.getBoolean("class-dumper.create-jar")).getOrElse(true);
            this.jarName = Try.of(() -> config.getString("class-dumper.jar-name")).getOrElse("instrumentedClasses");
        }

        public boolean isDumpEnabled() {
            return this.dumpEnabled;
        }
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    private Config getConfig() {
        return Try.of(() -> loadDefaultConfig().getConfig("kamon.agent"))
                .onFailure(missing -> LazyLogger.warn(() -> "It has not been found any configuration for Kamon Agent.", missing))
                .get();
    }

    private List<String> getInstrumentations(Config config) {
        return Try.of(() -> List.ofAll(config.getStringList("instrumentations")))
                .onFailure(exc -> LazyLogger.warn(() -> "The instrumentations have not been found. Perhaps you have forgotten to add them to the config?", exc))
                .getOrElse(Nil.instance());
    }

    private Option<String> getWithinConfiguration(Config config) {
        return Try.of(() -> List.ofAll(config.getStringList("within"))).map(within -> within.mkString("|")).toOption();
    }


    private Boolean getDebugMode(Config config) {
        return Try.of(() -> config.getBoolean("debug-mode")).getOrElse(false);
    }



    private Config loadDefaultConfig() {
        return ConfigFactory
                .load(this.getClass().getClassLoader(), ConfigParseOptions.defaults(), ConfigResolveOptions.defaults()
                .setAllowUnresolved(true));
    }
}
