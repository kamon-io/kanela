/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class AgentConfiguration {
    List<String> instrumentations;
    Option<String> withinPackage;
    Boolean debugMode;
    DumpConfig dump;

    private static class Holder {
        private static final AgentConfiguration Instance = new AgentConfiguration();
    }

    public static AgentConfiguration instance() {
        return Holder.Instance;
    }

    private AgentConfiguration() {
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
