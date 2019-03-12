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
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import kanela.agent.api.instrumentation.TypeTransformation;
import kanela.agent.bootstrap.dispatcher.Dispatcher;
import kanela.agent.util.classloader.ClassLoaderNameMatcher;
import kanela.agent.util.conf.KanelaConfiguration;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

public class InstrumentationRegistry extends AgentBuilder.Listener.Adapter {

    private final static String DISPATCHER_KEY = "InstrumentationRegistry";

    private Map<String, Map<TypeTransformation, List<TypeDescription>>> moduleTransformers = HashMap.empty();
    private Map<String, KanelaConfiguration.ModuleConfiguration> modulesConfiguration = HashMap.empty();
    private Map<String, String> modulesVersion = HashMap.empty();
    private Map<String, List<Throwable>> errors = HashMap.empty();

    public Map<String, Map<TypeTransformation, List<TypeDescription>>> getModuleTransformers() {
        return moduleTransformers;
    }

    public Option<String> moduleVersion(String moduleKey) {
        return modulesVersion.get(moduleKey);
    }
    public boolean isModuleActive(String moduleKey) {
        return moduleTransformers.getOrElse(moduleKey, HashMap.empty()).exists(t -> t._2.nonEmpty());
    }

    public Map<String, KanelaConfiguration.ModuleConfiguration> getModulesConfiguration() {
        return modulesConfiguration;
    }

    public Map<String, List<Throwable>> getErrors() {
        return errors;
    }

    public void register(KanelaConfiguration.ModuleConfiguration moduleDescription, TypeTransformation typeTransformation) {
        moduleTransformers = moduleTransformers.computeIfPresent(moduleDescription.getKey(),
                (mn, tts) -> tts.computeIfAbsent(typeTransformation, (k) -> List.empty())._2
        )._2;
        moduleTransformers = moduleTransformers.computeIfAbsent(moduleDescription.getKey(), (m) -> HashMap.of(typeTransformation, List.empty()))._2;
        modulesConfiguration = modulesConfiguration.computeIfAbsent(moduleDescription.getKey(), (m) -> moduleDescription)._2;
    }

    public static InstrumentationRegistry instance() {
        return Dispatcher.computeIfAbsent(
                InstrumentationRegistry.DISPATCHER_KEY,
                k -> new InstrumentationRegistry()
        );
    }

    public boolean applies(TypeTransformation tt, TypeDescription td, ClassLoader classLoader) {
        return tt.getElementMatcher().map(em -> em.matches(td)).getOrElse(false) &&
                ClassLoaderNameMatcher.RefinedClassLoaderMatcher.from(tt.getClassLoaderRefiner()).matches(classLoader);
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        moduleTransformers = moduleTransformers.map(
                (moduleName, transformations) -> Tuple.of(moduleName, transformations.map((transformation, typeDescriptions) -> {
                    if (applies(transformation, typeDescription, classLoader)) {
                        return Tuple.of(transformation, typeDescriptions.append(typeDescription));
                    } else {
                        return Tuple.of(transformation, typeDescriptions);
                    }
                })));
    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        errors = errors.computeIfPresent(typeName, (tn, errs) -> errs.append(throwable))._2;
        errors = errors.computeIfAbsent(typeName, (tn) -> List.of(throwable))._2;
    }

    public void registerModuleVersion(String moduleKey, Option<String> moduleVersion) {
        moduleVersion.forEach(version -> this.modulesVersion = this.modulesVersion.computeIfAbsent(moduleKey, k -> version)._2);
    }
}

