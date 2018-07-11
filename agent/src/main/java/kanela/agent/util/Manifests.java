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
import lombok.Value;
import lombok.val;

import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

@Value
public class Manifests {

    public static Set<String> getAllPropertiesFromTitleOrBundle() {
        return io.vavr.collection.List.ofAll(getAll())
                .flatMap(manifest -> filterMapByKey(manifest.getMainAttributes().entrySet()))
                .filter(Objects::nonNull)
                .toJavaSet();
    }

    private static List<String> filterMapByKey(Set<Map.Entry<Object,Object>> entries) {
        return entries.stream().filter(buildPredicate())
                .map(entry -> entry.getValue().toString())
                .collect(Collectors.toList());
    }

    private static Predicate<Map.Entry<Object, Object>> buildPredicate() {
        List<Predicate<Map.Entry<Object, Object>>> allPredicates = Arrays.asList(
                entry -> entry.getKey().toString().equalsIgnoreCase("Implementation-Title"),
                entry -> entry.getKey().toString().equalsIgnoreCase("Automatic-Module-Name"),
                entry -> entry.getKey().toString().equalsIgnoreCase("Bundle-Name")
        );
        return allPredicates.stream().reduce(w -> true, Predicate::or);
    }

    private static List<Manifest> getAll() {
        return Try.of(() -> {
            val resources = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
            final List<Manifest> manifests = new java.util.ArrayList<>(1);
            while (resources.hasMoreElements()) {
                val url = resources.nextElement();
                try(InputStream stream = url.openStream()) {
                    manifests.add(new Manifest(stream));
                }
            }
            return manifests;
        }).getOrElse(Collections::<Manifest>emptyList);
    }
}

