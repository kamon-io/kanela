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
import io.vavr.control.Try;
import kanela.agent.api.instrumentation.classloader.ClassRefiner;
import kanela.agent.util.log.Logger;
import lombok.Value;
import lombok.val;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.jar.asm.tree.ClassNode;

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
    private Set<String> fields;
    private Map<String, Set<String>> methodsWithArguments;

    public static ClassMatcher from(ClassRefiner refiner, ClassLoader loader)  {
        return Try.of(() -> {
            val target = refiner.getTarget();
            val resourceName = target.replace('.', '/') + ".class";

            try(InputStream in = loader.getResourceAsStream(resourceName)) {
                val classNode = convertToClassNode(in);
                return (ClassMatcher) new AnalyzedClass(refiner, extractFields(classNode), extractMethods(classNode));
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

    private Boolean containsFields(String... fields) {
        return this.fields.containsAll(Arrays.asList(fields));
    }

    private Boolean containsMethod(String methodName, String... parameters) {
        if(methodsWithArguments.containsKey(methodName)) {
            val parameterSet = methodsWithArguments.get(methodName);
            if(parameters.length > 0) return Arrays.asList(parameters).containsAll(parameterSet);
            return true;
        }
        return false;
    }

    private Predicate<Boolean> buildClassRefinerPredicate(ClassRefiner classRefiner) {
        java.util.List<Predicate<Boolean>> allPredicates = Arrays.asList(
                p -> containsFields(classRefiner.getFields().toArray(new String[0])),
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

    private static Set<String> extractFields(ClassNode classNode) {
        return List.ofAll(classNode.fields)
                .map(fieldNode -> fieldNode.name)
                .toJavaSet();
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
        val result = new ClassNode(Opcodes.ASM7);
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
