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

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
public class BuiltInModuleLoader {
    private static final Pattern pattern = Pattern.compile("kanela/agent/instrumentations/(.*).jar");
    //TODO:Improve this Please
    public static List<URL> getUrlModules() {
        return Jar.searchWith(pattern)
                .map(BuiltInModuleLoader::collectAll)
                .map(modules -> modules.stream().map(module -> Jar.getEmbeddedFile("/" + module).get()).collect(Collectors.toList()))
                .onFailure((cause) -> Logger.error(() -> "Error when trying to Load build-in instrumentation modules.", cause))
                .getOrElse(Collections.emptyList());
    }

    private static List<String> collectAll(List<String> modules) {
        return Lang.getRunningScalaVersion()
                .map(scalaVersion -> filterScalaModules(scalaVersion, modules))
                .getOrElse(modules.stream().filter(x -> !x.contains("_2.1")).collect(Collectors.toList()));
    }

    private static List<String> filterScalaModules(String scalaVersion, List<String> scalaModules) {
        return scalaModules.stream()
                .filter(moduleName -> moduleName.contains(scalaVersion))
                .collect(Collectors.toList());
    }
}