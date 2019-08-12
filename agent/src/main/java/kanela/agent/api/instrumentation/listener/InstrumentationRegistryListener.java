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

import static kanela.agent.util.classloader.ScalaCompilerClassLoaderMatcher.isScalaCompilerClassLoader;
import kanela.agent.api.instrumentation.TypeTransformation;
import kanela.agent.util.classloader.ClassLoaderNameMatcher;
import kanela.agent.util.conf.KanelaConfiguration;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captures and exposes the instrumentation modules known by Kanela. Since users of this class might be trying to access
 * the exposed data from a ClassLoader that is not connected to Kanela's (e.g. when running an application from SBT), we
 * will only expose data using plain Java types which are guaranteed to be loaded by the Bootstrap ClassLoader.
 *
 */
final public class InstrumentationRegistryListener extends AgentBuilder.Listener.Adapter {

    private static InstrumentationRegistryListener instance = new InstrumentationRegistryListener();
    public static InstrumentationRegistryListener instance() {
        return instance;
    }

    private Map<String, Map<TypeTransformation, List<TypeDescription>>> moduleTransformers = new ConcurrentHashMap<>();
    private Map<String, KanelaConfiguration.ModuleConfiguration> moduleConfigurations = new ConcurrentHashMap<>();
    private Map<String, List<Throwable>> errors = new ConcurrentHashMap<>();

    /**
     * Removes all collected information on this listener.
     */
    public void clear() {
        moduleTransformers = new ConcurrentHashMap<>();
        moduleConfigurations = new ConcurrentHashMap<>();
        errors = new ConcurrentHashMap<>();
    }

    /**
     * Registers a module. Registering a module makes the registry aware of its existence, but it does not mean that
     * any of the module's transformations have been applied. See onTransformation for more details.
     */
    public void register(KanelaConfiguration.ModuleConfiguration moduleConfig, TypeTransformation typeTransformation) {
        val moduleTransformations = moduleTransformers.computeIfAbsent(moduleConfig.getConfigPath(), k -> new ConcurrentHashMap<>());
        moduleTransformations.putIfAbsent(typeTransformation, Collections.synchronizedList(new LinkedList<>()));
        moduleConfigurations.put(moduleConfig.getConfigPath(), moduleConfig);
    }

    /**
     * We try to figure out if any applied transformation belongs to any of the known modules and if so, register what
     * type was transformed. This helps us figure out which modules are active since we will only consider a module to
     * be active once it has transformed a class.
     */
    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        moduleTransformers.values().forEach(moduleTypeTransformers -> {
            moduleTypeTransformers.forEach((typeTransformation, targetedTypeDescriptions) -> {

                if(isTargetedByTransformation(typeTransformation, typeDescription, classLoader)) {
                    targetedTypeDescriptions.add(typeDescription);
                }
            });
        });
    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        if(!isScalaCompilerClassLoader(classLoader)) {
            val typeErrors = errors.computeIfAbsent(typeName, (tn) -> Collections.synchronizedList(new LinkedList<>()));
            typeErrors.add(throwable);
        }
    }

    private boolean isTargetedByTransformation(TypeTransformation tt, TypeDescription td, ClassLoader classLoader) {
        return tt.getElementMatcher().map(em -> em.matches(td)).getOrElse(false) &&
            ClassLoaderNameMatcher.RefinedClassLoaderMatcher.from(tt.getClassLoaderRefiner()).matches(classLoader);
    }

    public boolean isModuleActive(String moduleKey) {
        val moduleTransformations = moduleTransformers.getOrDefault(moduleKey, Collections.emptyMap());
        return moduleTransformations.values().stream().anyMatch(tds -> !tds.isEmpty());
    }

    /**
     * Returns a list of all modules known to this registry, encoded with JDK-only types that can be safely shared
     * across classes loaded by different ClassLoaders. Each entry contains the following properties:
     *
     *   - configPath: The configuration path within "kanela.modules" on which the module was found.
     *   - name: The module's name.
     *   - description: The module's description.
     *   - enabled: Contains "true" or "false" to indicate whether the module will proceed to apply transformations if
     *     any of its target types are loaded.
     *   - active: Contains "true" or "false" to indicate whether the module has already applied transforamtion to any
     *     of its target types.
     */
    public static List<Map<String, String>> shareModules() {
        val modules = new LinkedList<Map<String, String>>();

        instance().moduleConfigurations.values().forEach(moduleConfig -> {
            val isActive = instance().isModuleActive(moduleConfig.getConfigPath());
            val moduleInfo = new HashMap<String, String>();

            moduleInfo.put("path", moduleConfig.getConfigPath());
            moduleInfo.put("name", moduleConfig.getName());
            moduleInfo.put("description", moduleConfig.getDescription());
            moduleInfo.put("enabled", String.valueOf(moduleConfig.isEnabled()));
            moduleInfo.put("active", String.valueOf(isActive));
            modules.add(moduleInfo);
        });

        return modules;
    }

    /**
     * Returns a map of target type to any exceptions that occurred while instrumenting that type.
     */
    public static Map<String, List<Throwable>> shareErrors() {
        return instance().errors;
    }

}

