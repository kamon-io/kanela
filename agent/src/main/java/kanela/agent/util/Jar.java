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

import io.vavr.control.Try;
import kanela.agent.Kanela;
import lombok.Value;
import lombok.val;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
public class Jar {

    public static Try<JarFile> getEmbeddedJar(String jarName) {
        return getEmbeddedFile(jarName).mapTry(file -> new JarFile(file.getFile()));
    }

    public static Try<URL> getEmbeddedFile(String jarName) {
        return Try.of(() -> {
            val tempFile = File.createTempFile(jarName, ".jar");
            tempFile.deleteOnExit();
            val resourceAsStream = Kanela.class.getResourceAsStream(jarName);
            Files.copy(resourceAsStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile.toURI().toURL();
        });
    }

    public static Try<List<ExtensionJar>> fromString(String arguments) {
        return stringToMap(arguments)
                .mapTry(map -> map.entrySet()
                        .stream()
                        .map(k -> ExtensionJar.from(k.getKey(), k.getValue()))
                        .collect(Collectors.toList()));
    }

    public static Try<List<String>> searchWith(Pattern pattern) {
       return getKanelaJar().mapTry(kanelaJar -> {
            val names = new ArrayList<String>();
            try (JarFile jarFile = new JarFile(kanelaJar)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    if (!pattern.matcher(jarEntry.getName()).matches()) continue;
                    names.add(jarEntry.getName());
                }
            }
            return names;
        });
    }

    private static Try<Map<String,String>> stringToMap(String value) {
        return Try.of(() -> Arrays.stream(value.split(";"))
                .map(s -> s.split(":"))
                .collect(Collectors.toMap(k -> k[0], v -> v[1])));
    }

    public static Try<String> getKanelaJar() {
        return Try.of(() -> {
            val filePath = Kanela.class.getProtectionDomain().getCodeSource()
                .getLocation()
                .getFile();

            return URLDecoder.decode(filePath, "UTF-8");
        });
    }

    @Value(staticConstructor = "from")
    static class ExtensionJar {
        String agentLocation;
        String classLoader;
    }
}
