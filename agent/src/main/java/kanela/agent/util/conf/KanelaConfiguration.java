/*
 * =========================================================================================
 * Copyright © 2013-2022 the kamon project <http://kamon.io/>
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

import java.util.Collections;
import java.util.HashMap;

import static io.vavr.API.*;


@Value
public class KanelaConfiguration {
    Boolean debugMode;
    DumpConfig dump;
    CircuitBreakerConfig circuitBreakerConfig;
    InstrumentationRegistryConfig instrumentationRegistryConfig;
    OldGarbageCollectorConfig oldGarbageCollectorConfig;
    Boolean showBanner;
    HashMap extraParams;
    Level logLevel;
    @Getter(AccessLevel.PRIVATE)
    Config config;

    synchronized public static KanelaConfiguration from(ClassLoader classLoader) {
        val config = new KanelaConfiguration(loadConfig(classLoader));
        latestInstance = config;
        return config;
    }

    private static KanelaConfiguration latestInstance = null;

    // TODO: Remove any access to this member.
    synchronized public static KanelaConfiguration instance() {
        if(latestInstance == null) {
            latestInstance = new KanelaConfiguration(loadConfig(Thread.currentThread().getContextClassLoader()));
        }

        return latestInstance;
    }

    private KanelaConfiguration(Config config) {
        this.config = config;
        this.debugMode = getDebugMode(config);
        this.showBanner = getShowBanner(config);
        this.extraParams = new HashMap();
        this.dump = new DumpConfig(config);
        this.circuitBreakerConfig = new CircuitBreakerConfig(config);
        this.instrumentationRegistryConfig = new InstrumentationRegistryConfig(config);
        this.oldGarbageCollectorConfig =  new OldGarbageCollectorConfig(config);
        this.logLevel = getLoggerLevel(config);
    }

    public List<ModuleConfiguration> getAgentModules() {
        val config = getConfig().getConfig("modules");
        Logger.debug(() -> "Loaded configuration => " + config.root().render());
        return List.ofAll(config.entrySet())
                .foldLeft(List.<String>empty(), (moduleList, moduleName) -> moduleList.append(moduleName.getKey().split("\\.")[0]))
                .toSet()
                .map(configPath -> {
                    Try<ModuleConfiguration> moduleSettings = Try.of(() -> {
                        val moduleConfig = config.getConfig(configPath);
                        val name = moduleConfig.getString("name");
                        val description = Try.of(() -> moduleConfig.getString("description")).getOrElse("");
                        val instrumentations = getInstrumentations(moduleConfig);
                        val within = getWithinConfiguration(moduleConfig);
                        val exclude = getExcludeConfiguration(moduleConfig);
                        val exceptionHandlerStrategy = Try.of(() -> moduleConfig.getString("exception-handler-strategy")).getOrElse("LOG");
                        val enabled = Try.of(() -> moduleConfig.getBoolean("enabled")).getOrElse(true);
                        val order = Try.of(() -> moduleConfig.getInt("order")).getOrElse(Integer.MAX_VALUE);
                        val stoppable = Try.of(() -> moduleConfig.getBoolean("stoppable")).getOrElse(false);
                        val bootstrapInjection = getBootstrapInjectionConfiguration(moduleConfig);
                        val enableClassFileVersionValidator = Try.of(() -> moduleConfig.getBoolean("enable-class-file-version-validator")).getOrElse(true);
                        val tempDirPrefix = Try.of(() -> moduleConfig.getString("temp-dir-prefix")).getOrElse("tmp");
                        val disableClassFormatChanges = Try.of(() -> moduleConfig.getBoolean("disable-class-format-changes")).getOrElse(false);

                        return ModuleConfiguration.from(
                                configPath, name,
                                description,
                                instrumentations,
                                within,
                                enabled,
                                order,
                                stoppable,
                                bootstrapInjection,
                                enableClassFileVersionValidator,
                                tempDirPrefix,
                                disableClassFormatChanges,
                                exclude,
                                exceptionHandlerStrategy);
                    });

                    moduleSettings.failed().forEach(cause -> Logger.warn(() -> "Malformed configuration for module on path: " + configPath + ". The module will be ignored.", cause));

                    return moduleSettings;

                })
                .filter(Try::isSuccess)
                .map(Try::get)
                .filter(module -> module.getInstrumentations().nonEmpty() && isEnabled(module))
                .toList()
                .sortBy(ModuleConfiguration::getOrder);
    }

    public boolean requiresBootstrapInjection() {
        return getAgentModules().exists(m -> m.isEnabled() && m.shouldInjectInBootstrap());
    }

    @Value(staticConstructor = "from")
    public static class ModuleConfiguration {
        String configPath;
        String name;
        String description;
        List<String> instrumentations;
        String withinPackage;
        boolean enabled;
        int order;
        boolean stoppable;
        BootstrapInjectionConfig bootstrapInjectionConfig;
        @Getter(AccessLevel.NONE)
        boolean enableClassFileVersionValidator;
        String tempDirPrefix;
        boolean disableClassFormatChanges;
        String excludePackage;
        String exceptionHandlerStrategy;

        public boolean shouldInjectInBootstrap() {
            return bootstrapInjectionConfig.enabled;
        }

        public boolean shouldValidateMinimumClassFileVersion() {
            return enableClassFileVersionValidator;
        }
    }

    @Value
    public static class DumpConfig {
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
    public static class InstrumentationRegistryConfig {
        boolean enabled;

        InstrumentationRegistryConfig(Config config) {
            this.enabled = Try.of(() -> config.getBoolean("instrumentation-registry.enabled")).getOrElse(false);
        }
    }

    @Value
    public class OldGarbageCollectorConfig {
        boolean shouldLogAfterGc;

        OldGarbageCollectorConfig(Config config) {
            this.shouldLogAfterGc = Try.of(() -> config.getBoolean("gc-listener.log-after-gc-run")).getOrElse(false);
        }

        public boolean isCircuitBreakerRunning() {
            return (boolean) KanelaConfiguration.this.getExtraParameter("circuit-breaker-running").getOrElse(false);
        }
    }

    @Value
    public static class BootstrapInjectionConfig {
        boolean enabled;
        List<String> helperClassNames;

        BootstrapInjectionConfig(boolean enabled, List<String> helperClassNames) {
            this.enabled = enabled;
            this.helperClassNames = helperClassNames;
        }

        BootstrapInjectionConfig(Config config) {
            this.enabled = Try.of(() -> config.getBoolean("enabled")).getOrElse(false);
            this.helperClassNames = List.ofAll(Try.of(() -> config.getStringList("helper-class-names")).getOrElse(Collections.emptyList()));
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

    private static Config loadConfig(ClassLoader classLoader) {
        return Try.of(() -> loadDefaultConfig(classLoader).getConfig("kanela"))
                .onFailure(missing -> Logger.error(() -> "It has not been found any configuration for Kanela Agent.", missing))
                .get();
    }

    private List<String> getInstrumentations(Config config) {
        return Try.of(() -> List.ofAll(config.getStringList("instrumentations")))
                .onFailure(exc -> Logger.warn(() -> "The instrumentations have not been found. Perhaps you have forgotten to add them to the config?", exc))
                .getOrElse(Nil.instance());
    }

    private String getWithinConfiguration(Config config) {
        if(config.hasPath("within"))
          return getTypeListPattern(config, "within").getOrElse("");
        return "";
    }

    private String getExcludeConfiguration(Config config) {
        if(config.hasPath("exclude"))
            return getTypeListPattern(config, "exclude").getOrElse("");
        return "";

    }

    private BootstrapInjectionConfig getBootstrapInjectionConfiguration(Config moduleConfig) {
        return Try.of(() -> new BootstrapInjectionConfig(moduleConfig.getConfig("bootstrap-injection"))).getOrElse(() -> new BootstrapInjectionConfig(false, List.empty()));
    }


    private Try<String> getTypeListPattern(Config config, String path) {
        return Try.of(() -> List.ofAll(config.getStringList(path)).mkString("|"));
    }

    private Boolean getDebugMode(Config config) {
        return Try.of(() -> config.getBoolean("debug-mode")).getOrElse(false);
    }

    private Boolean getShowBanner(Config config) {
        return Try.of(() -> config.getBoolean("show-banner")).getOrElse(false);
    }

    private static Config loadDefaultConfig(ClassLoader classLoader) {
        return ConfigFactory
                .load(classLoader, ConfigParseOptions.defaults(), ConfigResolveOptions.defaults()
                .setAllowUnresolved(true));
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
}
