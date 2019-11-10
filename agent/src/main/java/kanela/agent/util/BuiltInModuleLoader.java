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
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
public class BuiltInModuleLoader {
    private static final Pattern instrumentationPattern = Pattern.compile("kanela/agent/instrumentations/(.*).jar");
    private static final Pattern filterScalaPattern = Pattern.compile(".*_[0-9]\\.[0-9]+\\.jar");

    public static URL[] findModules() {
        return Jar.searchWith(instrumentationPattern)
                .map(BuiltInModuleLoader::collectAll)
                .map(BuiltInModuleLoader::urlsToJars)
                .onFailure((cause) -> Logger.error(() -> "Error when trying to Load build-in instrumentation modules.", cause))
                .getOrElse(new URL[]{});
    }

    private static URL[] urlsToJars(List<String> urls) {
        return io.vavr.collection.List.ofAll(urls)
                .map(url -> Jar.getEmbeddedFile("/" + url))
                .flatMap(Function.identity())
                .toJavaArray(URL[]::new);
    }
    private static List<String> collectAll(List<String> modules) {
        return Lang.getRunningScalaVersion()
                .map(scalaVersion -> filterScalaModules(scalaVersion, modules))
                .getOrElse(modules.stream().filter(moduleName -> filterScalaPattern.matcher(moduleName).matches()).collect(Collectors.toList()));
    }

    private static List<String> filterScalaModules(String scalaVersion, List<String> scalaModules) {
        return scalaModules.stream()
                .filter(moduleName -> moduleName.matches(scalaRegexVersion(scalaVersion)))
                .collect(Collectors.toList());
    }

    private static String scalaRegexVersion(String scalaVersion) {
        return ".*/[^/]*_" + scalaVersion.replace(".", "\\.") + "[^/]*.*";
    }
}