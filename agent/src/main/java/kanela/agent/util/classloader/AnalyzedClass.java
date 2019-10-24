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

package kanela.agent.util.classloader;

import io.vavr.Tuple;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import kanela.agent.api.instrumentation.classloader.ClassRefiner;
import kanela.agent.util.log.Logger;
import lombok.Value;
import lombok.val;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.jar.asm.tree.ClassNode;
import net.bytebuddy.utility.OpenedClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


interface ClassMatcher {
    Boolean match();
}

@Value
public class AnalyzedClass implements ClassMatcher {
    private ClassRefiner classRefiner;
    private Map<String, Object> fields;
    private Map<String, Set<String>> methodsWithArguments;

    public static ClassMatcher from(ClassRefiner refiner, ClassLoader loader)  {
        return Try.of(() -> {
            val target = refiner.getTarget();
            val resourceName = target.replace('.', '/') + ".class";

            try(InputStream in = loader.getResourceAsStream(resourceName)) {
                val classNode = convertToClassNode(in);
                return (ClassMatcher) new AnalyzedClass(refiner, extractFieldsAndValues(refiner, classNode, loader), extractMethods(classNode));
            }
        })
        .onFailure((cause) -> Logger.debug(() -> "Error trying to build an AnalyzedClass: " + cause.getMessage()))
        .getOrElse(new NoOpAnalyzedClass());
    }

    public Boolean match() {
        val evaluated = buildClassRefinerPredicate(this.classRefiner).test(true);
        if(!evaluated) Logger.debug(() -> "The Class: " + this.classRefiner.getTarget() + " was filtered because not match with the provided ClassRefined: " + this.classRefiner);
        return evaluated;
    }

    private Boolean containsFields(Map<String, Option<Object>> fieldsAndValues) {
        if(fieldsAndValues.isEmpty()) return true;
        return !fieldsAndValues
                .entrySet()
                .stream()
                .map((entry) -> containsField(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet())
                .contains(false);
    }

    private Boolean containsField(String fieldName, Option<Object> value) {
        return value.map(v -> v == fields.get(fieldName)).getOrElse(() -> fields.containsKey(fieldName));
    }

    private Boolean containsMethod(String methodName, String... parameters) {
        if (!methodsWithArguments.containsKey(methodName)) return false;
        val parameterSet = methodsWithArguments.get(methodName);
        if(parameters.length > 0) return Arrays.asList(parameters).containsAll(parameterSet);
        return true;
    }

    private Predicate<Boolean> buildClassRefinerPredicate(ClassRefiner classRefiner) {
        java.util.List<Predicate<Boolean>> allPredicates = Arrays.asList(
                p -> containsFields(classRefiner.getFields()),
                p -> containsMethodWithParameters(classRefiner.getMethods())
        );
        return allPredicates.stream().reduce(p -> true, Predicate::and);
    }

    private Boolean containsMethodWithParameters(Map<String, Set<String>> methods) {
        if (methods.isEmpty()) return true;
        return !methods.entrySet()
                .stream()
                .map((entry) ->  containsMethod(entry.getKey(), entry.getValue().toArray(new String[0])))
                .collect(Collectors.toSet())
                .contains(false);
    }

    private static Map<String, Object> extractFieldsAndValues(ClassRefiner refiner, ClassNode classNode, ClassLoader loader) {
        return List.ofAll(classNode.fields)
                .filter(fieldNode -> (fieldNode.access & Opcodes.ACC_SYNTHETIC) == 0)
                .toJavaMap(fieldNode -> Tuple.of(fieldNode.name, extractFieldValue(refiner, fieldNode.name, loader)));
    }

    private static Object extractFieldValue(ClassRefiner refiner, String fieldName, ClassLoader loader) {
        if(!refiner.getFields().getOrDefault(fieldName, Option.none()).isDefined()) return null;
        return Try.of(() -> {
            val clazz = Class.forName(refiner.getTarget(), true, loader);
            val instance = clazz.newInstance();
            val field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        }).onFailure((cause) -> Logger.debug(() -> "cannot get field value from: " + refiner.getTarget() + "." + fieldName))
          .getOrElse(() -> null);
    }

    private static Map<String, Set<String>> extractMethods(ClassNode classNode) {
        return List.ofAll(classNode.methods)
                .filter(methodNode -> (methodNode.access & Opcodes.ACC_SYNTHETIC) == 0)
                .toJavaMap(methodNode -> Tuple.of(methodNode.name, Array.of(Type.getArgumentTypes(methodNode.desc)).map(AnalyzedClass::getType).toJavaSet()));
    }

    private static String getType(Type methodDescription) {
        return methodDescription
                .getInternalName()
                .replace('/', '.');
    }

    private static ClassNode convertToClassNode(InputStream classBytes) throws IOException {
        val result = new ClassNode(OpenedClassReader.ASM_API);
        val reader =  new ClassReader(classBytes);
        reader.accept(result, ClassReader.SKIP_FRAMES);
        return result;
    }

    static class NoOpAnalyzedClass implements ClassMatcher {
        @Override
        public Boolean match() {
            return false;
        }
    }
}
