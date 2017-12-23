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


package kamon.agent.util;

import io.vavr.control.Try;
import kamon.agent.KamonAgent;
import lombok.Value;
import lombok.val;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;

@Value
public class Jar {

    public static Try<JarFile> getEmbeddedJar(String jarName) {
        return Try.of(() -> {
            val tempFile = File.createTempFile(jarName, ".jar");
            val resourceAsStream = KamonAgent.class.getResourceAsStream(jarName + ".jar");
            Files.copy(resourceAsStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new JarFile(tempFile);
        });
    }
}
