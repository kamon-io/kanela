/*
 *  ==========================================================================================
 *  Copyright © 2013-2025 The Kamon Project <https://kamon.io/>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 *  ==========================================================================================
 */

package kanela.agent;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;

public class Configuration {

  private final Agent agent;
  private final List<Configuration.Module> modules;

  public Configuration(Agent agent, List<Configuration.Module> modules) {
    this.agent = agent;
    this.modules = modules;
  }

  public Agent agent() {
    return agent;
  }

  public List<Module> modules() {
    return modules;
  }

  public static class Agent {
    private final String logLevel;
    private final boolean requiresBootstrapInjection;

    Agent(String logLevel, boolean requiresBootstrapInjection) {
      this.logLevel = logLevel;
      this.requiresBootstrapInjection = requiresBootstrapInjection;
    }

    public String logLevel() {
      return logLevel;
    }

    public boolean requiresBootstrapInjection() {
      return requiresBootstrapInjection;
    }
  }

  public static class Module {
    private final String key;
    private final String name;
    private final Optional<String> description;
    private final boolean enabled;
    private final int order;
    private final List<String> instrumentations;
    private final List<String> prefixes;
    private final Optional<List<String>> bootstrapPrefixes;
    private final Optional<List<String>> excludedPrefixes;

    Module(
        String key,
        String name,
        Optional<String> description,
        boolean enabled,
        int order,
        List<String> instrumentations,
        List<String> prefixes,
        Optional<List<String>> bootstrapPrefixes,
        Optional<List<String>> excludedPrefixes) {

      this.key = key;
      this.name = name;
      this.description = description;
      this.enabled = enabled;
      this.order = order;
      this.instrumentations = instrumentations;
      this.prefixes = prefixes;
      this.bootstrapPrefixes = bootstrapPrefixes;
      this.excludedPrefixes = excludedPrefixes;
    }

    public String key() {
      return key;
    }

    public String name() {
      return name;
    }

    public Optional<String> description() {
      return description;
    }

    public boolean enabled() {
      return enabled;
    }

    public int order() {
      return order;
    }

    public List<String> instrumentations() {
      return instrumentations;
    }

    public List<String> prefixes() {
      return prefixes;
    }

    public Optional<List<String>> bootstrapPrefixes() {
      return bootstrapPrefixes;
    }

    public Optional<List<String>> excludedPrefixes() {
      return excludedPrefixes;
    }
  }

  static Configuration createFrom(ClassLoader classLoader) {
    Config config =
        ConfigFactory.load(
            classLoader,
            ConfigParseOptions.defaults(),
            ConfigResolveOptions.defaults().setAllowUnresolved(true));

    if (!config.hasPath("kanela")) {
      throw new RuntimeException("Couldn't find kanela configuration in the classpath");
    }

    String logLevel = config.getString("kanela.log-level");
    String logFormat = "[{level}] {date} [kanela][{thread}] {message}";
    Configurator.defaultConfig().formatPattern(logFormat).activate();

    Logger.debug("Reading configuration from {}", () -> config.root().render());

    List<Module> modules = new ArrayList<Module>();
    if (!config.hasPath("kanela.modules")) {
      Logger.warn("No modules found in the kanela.modules. Kanela won't do anything");
    } else {
      Config modulesConfig = config.getConfig("kanela.modules");
      modulesConfig
          .root()
          .entrySet()
          .forEach(
              entry -> {
                try {
                  Config moduleConfig = modulesConfig.getConfig(entry.getKey());
                  String name = moduleConfig.getString("name");
                  Optional<String> description = optionalString(moduleConfig, "description");
                  boolean enabled = optionalBoolean(moduleConfig, "enabled").orElse(true);
                  int order = optionalNumber(moduleConfig, "order").orElse(1);
                  List<String> instrumentations = moduleConfig.getStringList("instrumentations");
                  List<String> prefixes = moduleConfig.getStringList("within");
                  Optional<List<String>> bootstrapPrefixes =
                      optionalStringList(moduleConfig, "within-bootstrap");
                  Optional<List<String>> excludedPrefixes =
                      optionalStringList(moduleConfig, "exclude");

                  modules.add(
                      new Module(
                          entry.getKey(),
                          name,
                          description,
                          enabled,
                          order,
                          instrumentations,
                          prefixes,
                          bootstrapPrefixes,
                          excludedPrefixes));
                } catch (Exception e) {
                  Logger.error(e, "Failed to read module configuration for {}", entry.getKey());
                }
              });
    }

    boolean requiresBootstrapInjection =
        modules.stream().anyMatch(m -> m.bootstrapPrefixes().isPresent() && m.enabled());

    Agent agent = new Agent(logLevel, requiresBootstrapInjection);
    return new Configuration(agent, modules);
  }

  private static Optional<String> optionalString(Config config, String path) {
    if (config.hasPath(path)) {
      return Optional.of(config.getString(path));
    } else return Optional.empty();
  }

  private static Optional<List<String>> optionalStringList(Config config, String path) {
    if (config.hasPath(path)) {
      return Optional.of(config.getStringList(path));
    } else return Optional.empty();
  }

  private static Optional<Boolean> optionalBoolean(Config config, String path) {
    if (config.hasPath(path)) {
      return Optional.of(config.getBoolean(path));
    } else return Optional.empty();
  }

  private static Optional<Integer> optionalNumber(Config config, String path) {
    if (config.hasPath(path)) {
      return Optional.of(config.getInt(path));
    } else return Optional.empty();
  }
}
