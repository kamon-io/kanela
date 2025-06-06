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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exposes Kanela's status using classes available in the JDK so that Kamon can read them and show
 * in its own Status API.
 */
public class StatusApi {

  private static Map<String, Map<String, String>> registeredModules = new ConcurrentHashMap<>();
  private static Map<String, Set<String>> activeModules = new ConcurrentHashMap<>();

  public static void registerModule(String key, String name, String description, boolean enabled) {
    Map<String, String> module = new HashMap<>();
    module.put("key", key);
    module.put("name", name);
    module.put("description", description);
    module.put("enabled", String.valueOf(enabled));
    registeredModules.put(key, module);
  }

  public static void clearModules() {
    registeredModules.clear();
    activeModules.clear();
  }

  public static void onTypeTransformed(String moduleKey, String typeName) {
    if (registeredModules.containsKey(moduleKey)) {
      Set<String> transformedTypes = activeModules.computeIfAbsent(moduleKey, k -> new HashSet<>());
      transformedTypes.add(typeName);
    }
  }

  /**
   * Returns a list of all modules known to this Kanela, encoded with JDK-only types that can be
   * safely shared across classes loaded by different ClassLoaders. Each entry contains the
   * following properties:
   *
   * <p>
   *
   * <ul>
   *   <li>configPath: The configuration path within "kanela.modules" on which the module was found.
   *   <li>name: The module's name.
   *   <li>description: The module's description.
   *   <li>enabled: Contains "true" or "false" to indicate whether the module will proceed to apply
   *       transformations if any of its target types are loaded.
   *   <li>active: Contains "true" or "false" to indicate whether the module has already applied
   *       transforamtion to any of its target types
   * </ul>
   */
  public static List<Map<String, String>> shareModulesInfo() {
    List<Map<String, String>> modules = new ArrayList<>();

    registeredModules
        .values()
        .forEach(
            module -> {
              Map<String, String> moduleInfo = new HashMap<>();
              String moduleKey = module.get("key");
              moduleInfo.put("path", moduleKey);
              moduleInfo.put("name", module.get("name"));
              moduleInfo.put("description", module.get("description"));
              moduleInfo.put("enabled", module.get("enabled"));
              moduleInfo.put("active", String.valueOf(activeModules.containsKey(moduleKey)));
              modules.add(moduleInfo);
            });

    return modules;
  }
}
