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


package kamon.agent.api.instrumentation.mixin;

import javaslang.control.Option;
import kamon.agent.api.instrumentation.Initializer;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class MixinDescription {

    Type implementation;
    Set<String> interfaces;
    String mixinClass;
    Option<String> mixinInit;
    ElementMatcher targetTypes;

    private MixinDescription(Type implementation, Set<String> interfaces, String mixinClass, Option<String> mixinInit, ElementMatcher targetTypes) {
        this.implementation = implementation;
        this.interfaces = interfaces;
        this.mixinClass = mixinClass;
        this.mixinInit = mixinInit;
        this.targetTypes = targetTypes;
    }

    public static MixinDescription of(ElementMatcher targetTypes, Class<?> clazz) {
        final Type implementation = Type.getType(clazz);
        final Set<String> interfaces = Arrays.stream(clazz.getInterfaces()).map(name -> Type.getType(name).getInternalName()).collect(Collectors.toSet());
        final Option<String> mixinInit = Option.ofOptional(Arrays.stream(clazz.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(Initializer.class)).findFirst().map(Method::getName));
        return new MixinDescription(implementation, interfaces, clazz.getName(), mixinInit, targetTypes);
    }

    public AgentBuilder.Transformer makeTransformer() {
        return (builder, typeDescription, classLoader) -> builder.visit(MixinClassVisitorWrapper.of(this));
    }
}