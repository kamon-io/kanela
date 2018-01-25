/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.agent.util.conf;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import io.vavr.collection.List;
import io.vavr.collection.List.Nil;
import io.vavr.control.Option;
import io.vavr.control.Try;
import kamon.agent.util.log.LazyLogger;
import lombok.*;
import org.pmw.tinylog.Level;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;

import static io.vavr.API.*;
import static java.text.MessageFormat.format;


@Value
public class AgentConfiguration {
    Boolean debugMode;
    DumpConfig dump;
    CircuitBreakerConfig circuitBreakerConfig;
    OldGarbageCollectorConfig oldGarbageCollectorConfig;
    Boolean showBanner;
    HashMap extraParams;
    Level logLevel;
    @Getter(AccessLevel.PRIVATE)
    Config config;

    private static class Holder {
        private static final AgentConfiguration Instance = new AgentConfiguration();
    }

    public static AgentConfiguration instance() {
        return Holder.Instance;
    }

    private AgentConfiguration() {
        this.config = loadConfig();
        this.debugMode = getDebugMode(config);
        this.showBanner = getShowBanner(config);
        this.extraParams = new HashMap();
        this.dump = new DumpConfig(config);
        this.circuitBreakerConfig = new CircuitBreakerConfig(config);
        this.oldGarbageCollectorConfig =  new OldGarbageCollectorConfig(config);
        this.logLevel = getLoggerLevel(config);
    }

    @SneakyThrows
    public List<ModuleConfiguration> getAgentModules() {
        val config = getConfig().getConfig("modules");
        return List.ofAll(config.entrySet())
                .foldLeft(List.<String>empty(), (moduleList, moduleName) -> moduleList.append(moduleName.getKey().split("\\.")[0]))
                .toSet()
                .map(moduleName -> {
                    val moduleConfig = config.getConfig(moduleName);
                    val name = moduleConfig.getString("name");
                    val instrumentations = getInstrumentations(moduleConfig);
                    val within = getWithinConfiguration(moduleConfig);
                    val order = Try.of(() -> moduleConfig.getInt("order")).getOrElse(1);
                    val stoppable = Try.of(() -> moduleConfig.getBoolean("stoppable")).getOrElse(false);
                    val injectInBootstrap = Try.of(() -> moduleConfig.getBoolean("inject-in-bootstrap")).getOrElse(false);
                    val tempDirPrefix = Try.of(() -> moduleConfig.getString("temp-dir-prefix")).getOrElse("tmp");

                    return ModuleConfiguration.from(name, instrumentations, within, order, stoppable, injectInBootstrap, createTempDirectory(tempDirPrefix));
                    })
                .filter(module -> module.getInstrumentations().nonEmpty())
                .toList()
                .sortBy(ModuleConfiguration::getOrder);
    }

    @Value(staticConstructor = "from")
    public static class ModuleConfiguration {
        String name;
        List<String> instrumentations;
        String withinPackage;
        int order;
        boolean stoppable;
        @Getter(AccessLevel.NONE)
        boolean injectInBootstrap;
        File tempDir;

        public boolean shouldInjectInBootstrap() {
            return injectInBootstrap;
        }
    }

    @Value
    public class DumpConfig {
        Boolean dumpEnabled;
        String dumpDir;
        Boolean createJar;
        String jarName;

        DumpConfig(Config config) {
            this.dumpEnabled = Try.of(() -> config.getBoolean("class-dumper.enabled")).getOrElse(false);
            this.dumpDir = Try.of(() -> config.getString("class-dumper.dir")).getOrElse( System.getProperty("user.home") + "/kamon-agent/dump");
            this.createJar = Try.of(() -> config.getBoolean("class-dumper.create-jar")).getOrElse(true);
            this.jarName = Try.of(() -> config.getString("class-dumper.jar-name")).getOrElse("instrumentedClasses");
        }

        public boolean isDumpEnabled() {
            return this.dumpEnabled;
        }
    }

    @Value
    public class CircuitBreakerConfig {
        boolean enabled;
        double freeMemoryThreshold;
        double gcProcessCPUThreshold;

        CircuitBreakerConfig(Config config) {
            this.enabled = Try.of(() -> config.getBoolean("circuit-breaker.enabled")).getOrElse(false);
            this.freeMemoryThreshold = Try.of(() -> config.getDouble("circuit-breaker.free-memory-threshold")).getOrElse(50.0);
            this.gcProcessCPUThreshold = Try.of(() -> config.getDouble("circuit-breaker.gc-process-cpu-threshold")).getOrElse(10.0);
        }

        public void circuitBreakerRunning() {
            AgentConfiguration.this.addExtraParameter("circuit-breaker-running", true);
        }
    }

    @Value
    public class OldGarbageCollectorConfig {
        boolean shouldLogAfterGc;

        OldGarbageCollectorConfig(Config config) {
            this.shouldLogAfterGc = Try.of(() -> config.getBoolean("gc-listener.log-after-gc-show")).getOrElse(false);
        }

        public boolean isCircuitBreakerRunning() {
            return (boolean) AgentConfiguration.this.getExtraParameter("circuit-breaker-running").getOrElse(false);
        }
    }


    public boolean isDebugMode() {
        return this.debugMode;
    }

    private <T> void addExtraParameter(String key, T value) {
        this.extraParams.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public  <T> Option<T> getExtraParameter(String key) {
        return Option.of((T) this.extraParams.get(key));
    }

    public boolean isAttachedInRuntime() {
        return (boolean) this.getExtraParameter("attached-in-runtime").getOrElse(false);
    }

    public void runtimeAttach() {
        this.addExtraParameter("attached-in-runtime", true);
    }

    private Config loadConfig() {
        return Try.of(() -> loadDefaultConfig().getConfig("kamon.agent"))
                .onFailure(missing -> LazyLogger.warn(() -> "It has not been found any configuration for Kamon Agent.", missing))
                .get();
    }

    private List<String> getInstrumentations(Config config) {
        return Try.of(() -> List.ofAll(config.getStringList("instrumentations")))
                .onFailure(exc -> LazyLogger.warn(() -> "The instrumentations have not been found. Perhaps you have forgotten to add them to the config?", exc))
                .getOrElse(Nil.instance());
    }

    private String getWithinConfiguration(Config config) {
        return Try
                .of(() -> List.ofAll(config.getStringList("within")).mkString("|"))
                .getOrElse(DefaultConfiguration.withinPackage);
    }


    private Boolean getDebugMode(Config config) {
        return Try.of(() -> config.getBoolean("debug-mode")).getOrElse(false);
    }

    private Boolean getShowBanner(Config config) {
        return Try.of(() -> config.getBoolean("show-banner")).getOrElse(false);
    }

    private Config loadDefaultConfig() {
        return ConfigFactory
                .load(this.getClass().getClassLoader(), ConfigParseOptions.defaults(), ConfigResolveOptions.defaults()
                .setAllowUnresolved(true));
    }

    private static File createTempDirectory(String tempDirPrefix) {
        return Try
                .of(() -> Files.createTempDirectory(tempDirPrefix).toFile())
                .getOrElseThrow(() -> new RuntimeException(format("Cannot create the temporary directory: {0}", tempDirPrefix)));
    }


    private Level getLoggerLevel(Config config) {
        val level = Try.of(() -> config.getString("log-level")).getOrElse("INFO");
        return Match(level).of(
                Case($("INFO"), Level.INFO),
                Case($("DEBUG"), Level.DEBUG),
                Case($("ERROR"), Level.ERROR),
                Case($("WARNING"), Level.WARNING),
                Case($("OFF"), Level.OFF)
            );
    }

    private static class DefaultConfiguration {
        static final String withinPackage = List.of(
                    "sun\\..*",
                    "com\\.sun\\..*",
                    "java\\..*",
                    "javax\\..*",
                    "org\\.aspectj.\\..*",
                    "com\\.newrelic.\\..*",
                    "org\\.groovy.\\..*",
                    "net\\.bytebuddy.\\..*",
                    "\\.asm.\\..*",
                    "kamon\\.agent\\..*",
                    "kamon\\.testkit\\..*",
                    "kamon\\.instrumentation\\..*",
                    "akka\\.testkit\\..*",
                    "org\\.scalatest\\..*",
                    "scala\\.(?!concurrent).*").mkString("|");
    }
}
