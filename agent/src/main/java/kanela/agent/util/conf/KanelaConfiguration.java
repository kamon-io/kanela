/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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

package kanela.agent.util.conf;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.List.Nil;
import io.vavr.control.Option;
import io.vavr.control.Try;
import kanela.agent.util.log.Logger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.val;
import org.pmw.tinylog.Level;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Function;

import static io.vavr.API.*;
import static java.text.MessageFormat.format;


@Value
public class KanelaConfiguration {
    Boolean debugMode;
    DumpConfig dump;
    CircuitBreakerConfig circuitBreakerConfig;
    OldGarbageCollectorConfig oldGarbageCollectorConfig;
    ClassReplacerConfig classReplacerConfig;
    Boolean showBanner;
    HashMap extraParams;
    Level logLevel;
    @Getter(AccessLevel.PRIVATE)
    Config config;

    private static class Holder {
        private static final KanelaConfiguration Instance = new KanelaConfiguration();
    }

    public static KanelaConfiguration instance() {
        return Holder.Instance;
    }

    private KanelaConfiguration() {
        this.config = loadConfig();
        this.debugMode = getDebugMode(config);
        this.showBanner = getShowBanner(config);
        this.extraParams = new HashMap();
        this.dump = new DumpConfig(config);
        this.circuitBreakerConfig = new CircuitBreakerConfig(config);
        this.oldGarbageCollectorConfig =  new OldGarbageCollectorConfig(config);
        this.classReplacerConfig =  new ClassReplacerConfig(config);
        this.logLevel = getLoggerLevel(config);
    }

    public List<ModuleConfiguration> getAgentModules() {
        val config = getConfig().getConfig("modules");
        Logger.debug(() -> "Loaded configuration => " + config.root().render());
        return List.ofAll(config.entrySet())
                .foldLeft(List.<String>empty(), (moduleList, moduleName) -> moduleList.append(moduleName.getKey().split("\\.")[0]))
                .toSet()
                .map(moduleName -> {
                    val moduleConfig = config.getConfig(moduleName);
                    val name = moduleConfig.getString("name");
                    val instrumentations = getInstrumentations(moduleConfig);
                    val within = getWithinConfiguration(moduleConfig);
                    val enabled = Try.of(() -> moduleConfig.getBoolean("enabled")).getOrElse(true);
                    val order = Try.of(() -> moduleConfig.getInt("order")).getOrElse(1);
                    val stoppable = Try.of(() -> moduleConfig.getBoolean("stoppable")).getOrElse(false);
                    val injectInBootstrap = Try.of(() -> moduleConfig.getBoolean("inject-in-bootstrap")).getOrElse(false);
                    val legacyBytecodeSupport = Try.of(() -> moduleConfig.getBoolean("legacy-bytecode-support")).getOrElse(false);
                    val tempDirPrefix = Try.of(() -> moduleConfig.getString("temp-dir-prefix")).getOrElse("tmp");
                    val disableClassFormatChanges = Try.of(() -> moduleConfig.getBoolean("disable-class-format-changes")).getOrElse(false);

                    return ModuleConfiguration.from(name, instrumentations, within, enabled, order, stoppable, injectInBootstrap, legacyBytecodeSupport, createTempDirectory(tempDirPrefix), disableClassFormatChanges);
                    })
                .filter(module -> module.getInstrumentations().nonEmpty())
                .filter(this::isEnabled)
                .toList()
                .sortBy(ModuleConfiguration::getOrder);
    }

    @Value(staticConstructor = "from")
    public static class ModuleConfiguration {
        String name;
        List<String> instrumentations;
        String withinPackage;
        boolean enabled;
        int order;
        boolean stoppable;
        @Getter(AccessLevel.NONE)
        boolean injectInBootstrap;
        @Getter(AccessLevel.NONE)
        boolean legacyBytecodeSupport;
        File tempDir;
        boolean disableClassFormatChanges;

        public boolean shouldInjectInBootstrap() {
            return injectInBootstrap;
        }

        public boolean shouldSupportLegacyBytecode() {
            return legacyBytecodeSupport;
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
            this.dumpDir = Try.of(() -> config.getString("class-dumper.dir")).getOrElse( System.getProperty("user.home") + "/kanela-agent/dump");
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
            KanelaConfiguration.this.addExtraParameter("circuit-breaker-running", true);
        }
    }

    @Value
    public class OldGarbageCollectorConfig {
        boolean shouldLogAfterGc;

        OldGarbageCollectorConfig(Config config) {
            this.shouldLogAfterGc = Try.of(() -> config.getBoolean("gc-listener.log-after-gc-show")).getOrElse(false);
        }

        public boolean isCircuitBreakerRunning() {
            return (boolean) KanelaConfiguration.this.getExtraParameter("circuit-breaker-running").getOrElse(false);
        }
    }

    @Value
    public class ClassReplacerConfig {
        List<String> classesToReplace;

        ClassReplacerConfig(Config config) {
            this.classesToReplace = List.ofAll(Try.of(() -> config.getStringList("class-replacer.replace")).getOrElse(Collections.emptyList()));
        }

        public io.vavr.collection.Map<String, String> classesToReplace() {
            return classesToReplace
                    .map(s -> s.split("=>"))
                    .map(classes -> Tuple.of(toInternalName(classes[0]), toInternalName(classes[1])))
                    .toMap(Function.identity());
        }

        private String toInternalName(String b) {
            return b.replace('.', '/');
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
        return Try.of(() -> loadDefaultConfig().getConfig("kanela"))
                .onFailure(missing -> Logger.error(() -> "It has not been found any configuration for Kanela Agent.", missing))
                .get();
    }

    private List<String> getInstrumentations(Config config) {
        return Try.of(() -> List.ofAll(config.getStringList("instrumentations")))
                .onFailure(exc -> Logger.warn(() -> "The instrumentations have not been found. Perhaps you have forgotten to add them to the config?", exc))
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
                .load(Thread.currentThread().getContextClassLoader(), ConfigParseOptions.defaults(), ConfigResolveOptions.defaults()
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
                Case($("TRACE"), Level.TRACE),
                Case($("OFF"), Level.OFF)
            );
    }

    private boolean isEnabled(ModuleConfiguration module) {
        if (module.enabled) return true;
        Logger.info(() -> "The Module: " + module.getName() + " is disabled");
        return false;
    }

    private static class DefaultConfiguration {
        static final String withinPackage = List.of(
                    "(?!sun\\..*)",
                    "(?!com\\.sun\\..*)",
                    "(?!java\\..*)",
                    "(?!javax\\..*)",
                    "(?!org\\.aspectj.\\..*)",
                    "(?!com\\.newrelic.\\..*)",
                    "(?!org\\.groovy.\\..*)",
                    "(?!net\\.bytebuddy.\\..*)",
                    "(?!\\.asm.\\..*)",
                    "(?!kanela\\.agent\\..*)",
                    "(?!kamon\\.testkit\\..*)",
                    "(?!kamon\\.instrumentation\\..*)",
                    "(?!akka\\.testkit\\..*)",
                    "(?!org\\.scalatest\\..*)",
                    "(?!scala\\.(?!concurrent).*)").mkString("", "", ".*");
    }
}
