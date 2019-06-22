/*
 * =========================================================================================
 * Copyright © 2013-2019 the kamon project <http://kamon.io/>
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
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
public class BootstrapInjector {

    public static void injectJar(Instrumentation instrumentation, String jarName) {
        val jarFile = Jar.getEmbeddedJar(jarName + ".jar")
                .onFailure(error -> Logger.error(error::getMessage, error))
                .get();

        instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
    }

    public static void inject(File folder, Instrumentation instrumentation, List<String>  allClasses) {
        ClassInjector.UsingInstrumentation
                .of(folder, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation)
                .injectRaw(getTypeDefinitions(allClasses));
    }

    private static Map<String, byte[]> getTypeDefinitions(List<String> helperClassNames)  {
        return helperClassNames.stream().collect(Collectors.toMap(k -> k, BootstrapInjector::getClassBytes));
    }

    private static byte[] getClassBytes(String className)  {
        return Try.of(() -> ClassFileLocator.ForClassLoader.of(ClassLoader.getSystemClassLoader()).locate(className).resolve())
                .onFailure(error -> Logger.error(error::getMessage, error))
                .getOrElseThrow((cause) -> new RuntimeException("Could not locate class: " + className));
    }
}
