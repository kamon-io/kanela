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

package kanela.agent.util;

import kanela.agent.util.log.Logger;
import lombok.Value;
import lombok.val;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
public class BuildInModuleLoader {
    private static final Pattern pattern = Pattern.compile("kanela/agent/instrumentations/(.*).jar");

    public static void attach(Instrumentation instrumentation) {
        Jar.searchWith(pattern).andThen(() -> Logger.info(() -> "BuildIn Module Loader activated."))
                .onSuccess(modules -> loadAll(modules, instrumentation))
                .onFailure((cause) -> Logger.error(() -> "Error when trying to Load build-in instrumentation modules.", cause));
    }

    private static void loadAll(List<String> modules, Instrumentation instrumentation) {
        //TODO:improve this please!!!!
        val partition = modules.stream().collect(Collectors.partitioningBy(x -> x.contains("_2.1")));

        loadJavaModules(partition.get(false), instrumentation);
        Lang.getRunningScalaVersion().forEach(x -> loadScalaModules(x, partition.get(true), instrumentation));
    }

    private static void loadJavaModules(List<String> javaModules, Instrumentation instrumentation) {
        loadModules(javaModules, instrumentation);
    }

    private static void loadScalaModules(String scalaVersion, List<String> scalaModules, Instrumentation instrumentation) {
        val modules = scalaModules
                .stream()
                .filter(moduleName -> moduleName.contains(scalaVersion))
                .collect(Collectors.toList());

        loadModules(modules, instrumentation);
    }

    private static void loadModules(List<String> modules, Instrumentation instrumentation) {
        modules.stream()
                .map(module -> Jar.getEmbeddedJar("/" + module))
                .forEach(jarModule -> jarModule.onSuccess(instrumentation::appendToSystemClassLoaderSearch)
                        .andThen(jarFile -> Logger.info(() -> "Kamon instrumentation module: " + jarFile.getName() + " added to SystemClassLoader"))
                        .onFailure((cause) -> Logger.error(() -> "Error when trying to append module to SystemClassLoader.", cause)));
    }
}