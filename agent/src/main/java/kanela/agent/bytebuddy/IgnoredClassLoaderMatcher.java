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

package kanela.agent.bytebuddy;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.bytebuddy.matcher.ElementMatcher.Junction;

public class IgnoredClassLoaderMatcher extends Junction.AbstractBase<ClassLoader> {

  private final Map<ClassLoader, Boolean> knownClassLoaders =
      Collections.synchronizedMap(new WeakHashMap<>());

  private static final Set<String> ignored = ConcurrentHashMap.newKeySet();

  static {
    ignored.add("sun.reflect.DelegatingClassLoader");
    ignored.add("org.codehaus.groovy.runtime.callsite.CallSiteClassLoader");
    ignored.add("xsbt.boot.LibraryClassLoader");
    ignored.add("sbt.internal.PluginManagement$PluginClassLoader");
    ignored.add("sbt.internal.classpath.ClassLoaderCache$Key$CachedClassLoader");
    ignored.add("com.lightbend.lagom.dev.NamedURLClassLoader");
    ignored.add("com.lightbend.lagom.sbt.SbtKanelaRunnerLagom$LagomServiceLocatorClassLoader");
  }

  @Override
  public boolean matches(ClassLoader target) {
    return target != null
        && knownClassLoaders.computeIfAbsent(
            target,
            k -> {
              return ignored.contains(target.getClass().getName());
            });
  }
}
