/*
 * =========================================================================================
 * Copyright © 2013-2025 the kamon project <http://kamon.io/>
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

package kanela.agent.bootstrap;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;

public class BootstrapInjector {

  public static void inject(Instrumentation instrumentation, List<String> allClasses) {
    ClassInjector.UsingUnsafe.Factory.resolve(instrumentation)
        .make(null, null)
        .injectRaw(getClassBytes(allClasses));
  }

  private static Map<String, byte[]> getClassBytes(List<String> helperClassNames) {
    return helperClassNames.stream()
        .collect(
            Collectors.toMap(
                className -> className,
                className -> {
                  try {
                    return ClassFileLocator.ForClassLoader.of(ClassLoader.getSystemClassLoader())
                        .locate(className)
                        .resolve();
                  } catch (Exception e) {
                    throw new RuntimeException(
                        "Could not locate class for Bootstrap injection: " + className, e);
                  }
                }));
  }
}
