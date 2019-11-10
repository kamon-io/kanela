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

package kanela.agent.util.classloader;

import io.vavr.control.Option;
import io.vavr.control.Try;
import kanela.agent.util.BuiltInModuleLoader;
import kanela.agent.util.Lang;
import lombok.val;

import java.util.function.Consumer;

/**
 * ClassPath utility used when loading instrumentation modules and configuration. The goal of this class is creating a
 * special ClassLoader instance that provides two Kanela-specific capabilities:
 *
 *   1. It is able to load instrumentation modules embedded in the Kanela jar itself. All jar files that are included
 *      under the "kanela/agent/instrumentations" folder on the Kanela jar will be unpacked and applied.
 *
 *   2. It searches for an instrumentation ClassLoader to use when loading instrumentations. This ClassLoader is useful
 *      when attaching the Kanela agent at runtime while using tools and frameworks with intricate class loading
 *      mechanisms like SBT and Play Framework on development mode. This allows users to specify which of the several
 *      existent ClassLoaders should be used by Kanela to load instrumentations.
 *
 * The instrumentation ClassLoader instance is shared via the "kanela.instrumentation.classloader" system property,
 * which should be set by the infrastructure code (usually a SBT or toolkit-specific plugin) before attaching/reloading
 * the Kanela agent and will be cleared out as soon as this class' construction finishes.
 *
 * NOTE: Even though system properties are not meant to be used as an object instance sharing mechanism, after carefully
 * reviewing the options and warnings on the Properties class, it turns out to be the most simple and convenient way to
 * share the instrumentation ClassLoader instance.
 */
public class InstrumentationClassPath {

    private final ClassLoader classLoader;
    private static final String SecondaryClassLoaderPropertyName = "kanela.instrumentation.classLoader";
    private static volatile InstrumentationClassPath lastClassPath = null;

    private InstrumentationClassPath() {
        val builtInModules = BuiltInModuleLoader.findModules();

        classLoader = findSecondaryClassLoader()
            .map(cl -> new ChildFirstURLClassLoader(builtInModules, cl))
            .getOrElse(() -> new ChildFirstURLClassLoader(builtInModules, getParentClassLoader()));
    }

    private Option<ClassLoader> findSecondaryClassLoader() {
        return Try.of(() -> (ClassLoader) System.getProperties().get(SecondaryClassLoaderPropertyName))
            .andFinally(() -> System.getProperties().remove(SecondaryClassLoaderPropertyName))
            .toOption();
    }

    public static InstrumentationClassPath build() {
        lastClassPath = new InstrumentationClassPath();
        return lastClassPath;
    }

    public static Option<InstrumentationClassPath> last() {
      return Option.of(lastClassPath);
    }

    public ClassLoader getClassLoader() {
      return classLoader;
    }

    public void use(Consumer<ClassLoader> thunk) {
        val oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            thunk.accept(classLoader);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private ClassLoader getParentClassLoader() {
        return Try.of(() -> {
            val javaVersion = Lang.getRunningJavaVersion();
            if (javaVersion.startsWith("1.7") || javaVersion.startsWith("1.8")) return null;
            //platform classloader is parent of system in java >= 9
            val method = ClassLoader.class.getDeclaredMethod("getPlatformClassLoader");
            return (ClassLoader) method.invoke(null);
        }).getOrElse(() -> null);
    }
}
