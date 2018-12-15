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

package kanela.agent.api.instrumentation.listener;

import kanela.agent.api.instrumentation.KanelaInstrumentation;
import kanela.agent.api.instrumentation.TypeTransformation;
import kanela.agent.util.classloader.ClassLoaderNameMatcher;
import kanela.agent.util.log.Logger;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.util.HashMap;
import java.util.Map;

import static java.text.MessageFormat.format;

@EqualsAndHashCode(callSuper = false)
public class DamInstrumentationListener extends AgentBuilder.Listener.Adapter {

    private static DamInstrumentationListener _instance;

    private Map<String, TypeTransformation> moduleTransformers = new HashMap<>();

    public void add(String moduleName, TypeTransformation typeTransformation) {
        moduleTransformers.put(moduleName, typeTransformation);
    }

    // needed for a single instance between classloaders
    private static Class getClass(String classname)
            throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if(classLoader == null)
            classLoader = DamInstrumentationListener.class.getClassLoader();
        return (classLoader.loadClass(classname));
    }

    public static DamInstrumentationListener instance() {
        if(_instance == null) {
            try {
                _instance = (DamInstrumentationListener) getClass("kanela.agent.api.instrumentation.listener.DamInstrumentationListener").newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return _instance;
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        for (Map.Entry<String, TypeTransformation> e : moduleTransformers.entrySet()) {
            if (
                    e.getValue().getElementMatcher().map(em -> em.matches(typeDescription)).getOrElse(false) &&
                            ClassLoaderNameMatcher.RefinedClassLoaderMatcher.from(e.getValue().getClassLoaderRefiner()).matches(classLoader)
            ) {
                Logger.info(() -> format("++++++++++++++++> ({3}) Transformed => {0} and loaded from {1} and {2}", typeDescription, (classLoader == null) ? "Bootstrap class loader" : classLoader.getClass().getName(), dynamicType.toString(), e.getKey()));

            }
        }
    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        Logger.error(() -> format("!!!!!!!!!!!!!!!!> Error for: {0}", typeName), throwable);
    }
}
