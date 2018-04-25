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
import kanela.agent.util.log.Logger;
import lombok.Value;
import lombok.val;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.jar.JarFile;

@Value
public class ExtensionLoader {

    public static void attach(String args, Instrumentation instrumentation) {
        if(args != null && !args.isEmpty()) {
            Jar.fromString(args)
                    .onSuccess(extensions -> install(extensions, instrumentation))
                    .andThen(() -> Logger.info(() -> "Extension Loader activated."))
                    .onFailure((cause) -> Logger.error(() -> "Error when trying to install an extension.", cause));
        }
    }

    private static void install(List<Jar.ExtensionJar> extensions, Instrumentation instrumentation) {
        extensions.forEach(extension -> {
            val classLoader = Try.of(extension::getClassLoader).getOrElse("system");
            if(classLoader.equalsIgnoreCase("system")) appendToSystemClassloader(extension.getAgentLocation(), instrumentation);
            if(classLoader.equalsIgnoreCase("bootstrap")) appendToBootstrapClassloader(extension.getAgentLocation(), instrumentation);
        });
    }

    private static void appendToSystemClassloader(String agentLocation, Instrumentation instrumentation) {
        Logger.debug(() -> "append jar to SystemClassloader ==> " + agentLocation);
        Try.run(() -> instrumentation.appendToSystemClassLoaderSearch(new JarFile(agentLocation)));
    }

    private static void appendToBootstrapClassloader(String agentLocation, Instrumentation instrumentation) {
        Logger.debug(() -> "append jar to BootstrapClassloader ==> " + agentLocation);
        Try.run(() -> instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(agentLocation)));
    }
}