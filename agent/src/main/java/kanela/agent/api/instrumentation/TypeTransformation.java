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

package kanela.agent.api.instrumentation;

import io.vavr.control.Option;
import kanela.agent.api.instrumentation.classloader.ClassLoaderRefiner;
import lombok.Value;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class TypeTransformation {
    String instrumentationName;
    Option<ElementMatcher<? super TypeDescription>> elementMatcher;
    Option<ClassLoaderRefiner> classLoaderRefiner;
    List<AgentBuilder.Transformer> bridges;
    List<AgentBuilder.Transformer> mixins;
    List<AgentBuilder.Transformer> transformations;

    @SafeVarargs
    static TypeTransformation of(String instrumentationName,
                                 Option<ElementMatcher<? super TypeDescription>> elementMatcher,
                                 Option<ClassLoaderRefiner> classLoaderRefiner,
                                 List<AgentBuilder.Transformer> bridges,
                                 List<AgentBuilder.Transformer> mixins,
                                 List<AgentBuilder.Transformer>... transformers) {

        val transformations = Arrays.stream(transformers)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        return new TypeTransformation(instrumentationName ,elementMatcher, classLoaderRefiner, bridges, mixins, transformations);
    }

    public Boolean isActive() {
        return true;
    }
}
