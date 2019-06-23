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


package kanela.agent.api.instrumentation.mixin;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Value;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

@Value
public class MixinDescription {

    Class<?> mixinClass;

    public static MixinDescription of(Class<?> clazz) {
        return new MixinDescription(clazz);
    }

    public AgentBuilder.Transformer makeTransformer() {
        return (builder, typeDescription, classLoader, module) -> {
            val interfaces = List.ofAll(Arrays.asList(mixinClass.getInterfaces()))
                .toSet()
                .map(TypeDescription.ForLoadedType::new)
                .toJavaList();

            return builder
                .implement(interfaces)
                .visit(MixinClassVisitorWrapper.of(this, typeDescription, classLoader));
        };

    }

    public Option<String> getInitializerMethod() {
        return Option.ofOptional(Arrays.stream(mixinClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Initializer.class))
            .findFirst()
            .map(Method::getName));
    }
}