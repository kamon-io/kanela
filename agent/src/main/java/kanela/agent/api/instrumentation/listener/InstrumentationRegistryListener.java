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

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import kanela.agent.api.instrumentation.TypeTransformation;
import kanela.agent.util.classloader.ClassLoaderNameMatcher;
import kanela.agent.util.log.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import io.vavr.collection.List;
import io.vavr.collection.Map;

import static java.text.MessageFormat.format;

public class InstrumentationRegistryListener extends AgentBuilder.Listener.Adapter {

    private static InstrumentationRegistryListener _instance;

    private Map<String, List<Tuple2<TypeTransformation, Boolean>>> moduleTransformers = HashMap.empty();

    public Map<String, Map<String, Boolean>> getRecorded() {
        return moduleTransformers.mapValues(value -> value.toMap((t) -> Tuple.of(t._1.getInstrumentationName(), t._2)));
    }

    public void register(String moduleName, TypeTransformation typeTransformation) {
        moduleTransformers = moduleTransformers.computeIfPresent(moduleName, (mn, tts) -> tts.append(Tuple.of(typeTransformation, false)))._2;
        moduleTransformers = moduleTransformers.computeIfAbsent(moduleName, (m) -> List.of(Tuple.of(typeTransformation, false)))._2;
    }

    // needed for a single instance between classloaders
    private static Class getClass(String classname)
            throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if(classLoader == null)
            classLoader = InstrumentationRegistryListener.class.getClassLoader();
        return (classLoader.loadClass(classname));
    }

    public static InstrumentationRegistryListener instance() {
        if(_instance == null) {
            try {
                _instance = (InstrumentationRegistryListener) getClass("kanela.agent.api.instrumentation.listener.InstrumentationRegistryListener").newInstance();
            } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return _instance;
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        moduleTransformers.map(
                (moduleName, transformations) -> Tuple.of(moduleName, transformations.map(transformation -> {
                    if (transformation._1.getElementMatcher().map(em -> em.matches(typeDescription)).getOrElse(false) &&
                        ClassLoaderNameMatcher.RefinedClassLoaderMatcher.from(transformation._1.getClassLoaderRefiner()).matches(classLoader)
                    ) {
                        Logger.info(() -> format("++++++++++++++++> ({3} - {4} - {5}) Transformed => {0} and loaded from {1} and {2}",
                                typeDescription,
                                (classLoader == null) ? "Bootstrap class loader" : classLoader.getClass().getName(),
                                dynamicType.toString(),
                                moduleName,
                                transformation._1.getInstrumentationName(),
                                transformation._1.getTransformations().size() + transformation._1.getBridges().size() + transformation._1.getMixins().size()));
                        return transformation.update2(true);
                    } else {
                        return transformation;
                    }
                })));
    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        Logger.error(() -> format("!!!!!!!!!!!!!!!!> Error for: {0}", typeName), throwable);
    }
}
