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

package kanela.agent.api.instrumentation.classloader;

import java.util.Arrays;
import java.util.List;

public interface ClassLoaderRefiner {
    List<ClassRefiner> refiners();

    static ClassLoaderRefiner from(ClassRefiner... refiners) {
        return () -> Arrays.asList(refiners);
    }

    static ClassLoaderRefiner mustContains(String... classes) {
        return from(io.vavr.collection.List.of(classes)
                .map(clazz -> ClassRefiner.builder().mustContain(clazz).build())
                .toJavaArray(ClassRefiner[]::new));
    }
}