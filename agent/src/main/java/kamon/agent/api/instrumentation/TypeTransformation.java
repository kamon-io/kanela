/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

package kamon.agent.api.instrumentation;

import javaslang.collection.HashSet;
import javaslang.control.Option;
import lombok.Value;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class TypeTransformation {

    Option<ElementMatcher<? super TypeDescription>> elementMatcher;
    javaslang.collection.Set<AgentBuilder.Transformer> mixins;
    javaslang.collection.Set<AgentBuilder.Transformer> transformations;

    @SafeVarargs
    static TypeTransformation of(Option<ElementMatcher<? super TypeDescription>> elementMatcher, Set<AgentBuilder.Transformer> mixins, Set<AgentBuilder.Transformer>... transformers) {
        val transformations = Arrays.stream(transformers).flatMap(Collection::stream).collect(Collectors.toSet());
        return new TypeTransformation(elementMatcher, HashSet.ofAll(mixins),HashSet.ofAll(transformations));
    }

    public Boolean isActive() {
        return true;
    }
}
