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

import io.vavr.control.Option;
import io.vavr.control.Try;
import kanela.agent.api.instrumentation.classloader.ClassLoaderRefiner;
import kanela.agent.api.instrumentation.classloader.ClassRefiner;
import kanela.agent.util.collection.ConcurrentReferenceHashMap;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.tree.ClassNode;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Value
@EqualsAndHashCode(callSuper = false)
public class ClassLoaderNameMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

    String name;

    private ClassLoaderNameMatcher(String name) {
        this.name = name;
    }

    public static ElementMatcher.Junction.AbstractBase<ClassLoader> withName(String name) {
        return new ClassLoaderNameMatcher(name);
    }

    public static ElementMatcher.Junction.AbstractBase<ClassLoader> isReflectionClassLoader() {
        return new ClassLoaderNameMatcher("sun.reflect.DelegatingClassLoader");
    }

    public static ElementMatcher.Junction.AbstractBase<ClassLoader> isGroovyClassLoader() {
        return new ClassLoaderNameMatcher("org.codehaus.groovy.runtime.callsite.CallSiteClassLoader");
    }

    public static ElementMatcher.Junction.AbstractBase<ClassLoader> isKanelaClassLoader() {
        return new ClassLoaderNameMatcher(ChildFirstURLClassLoader.class.getName());
    }

    public static ElementMatcher.Junction.AbstractBase<ClassLoader> containsClasses(final String... names) {
        return ClassLoaderHasClassMatcher.from(names);
    }

    public static ElementMatcher.Junction.AbstractBase<ClassLoader> hasClassWithField(final String className, final String fieldName) {
        return ClassLoaderHasClassWithFieldMatcher.from(className, fieldName);
    }

    public static ElementMatcher.Junction.AbstractBase<ClassLoader> hasClassWithMethod(final String className, final String methodName, final String... methodArgs) {
        return ClassLoaderHasClassWithMethodMatcher.from(className, methodName, methodArgs);
    }


    @Override
    public boolean matches(ClassLoader target) {
        return target != null && name.equals(target.getClass().getName());
    }

    @Value(staticConstructor = "from")
    @EqualsAndHashCode(callSuper = false)
    public static class ClassLoaderHasClassMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

        ConcurrentReferenceHashMap<ClassLoader, Boolean> cache = new ConcurrentReferenceHashMap<>();

        String classes[];

        @Override
        public boolean matches(final ClassLoader target) {
            if (target == null) return false;
//            return cache.computeIfAbsent(target, (key) -> Arrays.stream(classes).anyMatch(name -> target.getResource(getResourceName(name)) == null));
//            Arrays.stream(classes).forEach(name -> target.getResource(getResourceName(name)).get);
            return cache.computeIfAbsent(target, (key) ->
                    Arrays.stream(classes).anyMatch(name -> target.getResource(getResourceName(name)) == null));
        }

        private static String getResourceName(final String className) {
            if (!className.endsWith(".class")) {
                return className.replace('.', '/') + ".class";
            } else {
                return className;
            }
        }

        public static ClassNode convertToClassNode(byte[] classBytes) {
            ClassNode result = new ClassNode(Opcodes.ASM6);
            ClassReader reader = new ClassReader(classBytes);
            reader.accept(result, ClassReader.SKIP_FRAMES);
            return result;
        }
    }

    @Value(staticConstructor = "from")
    @EqualsAndHashCode(callSuper = false)
    public static class ClassLoaderHasClassWithFieldMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

        ConcurrentReferenceHashMap<ClassLoader, Boolean> cache = new ConcurrentReferenceHashMap<>();

        String className;
        String fieldName;

        @Override
        public boolean matches(final ClassLoader target) {
            if (target == null) return false;
            return cache.computeIfAbsent(target, (key) -> Try.of(() -> {
                val clazz = Class.forName(className, false, target);
                clazz.getDeclaredField(fieldName);
                return true;
            }).getOrElse(false));
        }
    }


    @Value(staticConstructor = "from")
    @EqualsAndHashCode(callSuper = false)
    public static class ClassLoaderHasClassWithMethodMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

        ConcurrentReferenceHashMap<ClassLoader, Boolean> cache = new ConcurrentReferenceHashMap<>();

        String className;
        String methodName;
        String[] methodArgs;

        @Override
        public boolean matches(final ClassLoader target) {
            if (target == null) return false;
            return cache.computeIfAbsent(target, (key) -> Try.of(() -> {
                val clazz = Class.forName(className, false, target);
                val methodArgsClasses = new Class[methodArgs.length];

                for (int i = 0; i < methodArgs.length; ++i) {
                    methodArgsClasses[i] = target.loadClass(methodArgs[i]);
                }

                if (clazz.isInterface()) clazz.getMethod(methodName, methodArgsClasses);
                else clazz.getDeclaredMethod(methodName, methodArgsClasses);
                return true;
            }).getOrElse(false));
        }
    }


    @Value(staticConstructor = "from")
    @EqualsAndHashCode(callSuper = false)
    public static class RefinedClassLoaderMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

        ConcurrentReferenceHashMap<ClassLoader, Boolean> cache = new ConcurrentReferenceHashMap<>();

        Option<ClassLoaderRefiner> refiner;

        @Override
        @SneakyThrows
        public boolean matches(final ClassLoader target) {
            if (target == null) return false;
            return cache.computeIfAbsent(target, (key) -> Try.of(() -> {
                return refiner.map(r -> {
                     return io.vavr.collection.List.ofAll(r.refiners()).map(rr -> {
                        Main.AnalyzedClass analyzedClass = Main.AnalyzedClass.from(rr.getTarget(), target);

                         Predicate<Boolean> booleanPredicate = analyzedClass.buildPredicate(rr.getTarget(), rr.getFields(), rr.getMethods());
                         System.out.println("predicate => " + booleanPredicate.test(true));
                         return booleanPredicate.test(true);
                    }).getOrElse(false);
                }).getOrElse(false);
            }).getOrElse(false));
        }
    }
}

//                return refiner.map(r -> {
//                    val refiners = r.refiners().forEach(rr -> {
//                        Main.AnalyzedClass analyzedClass = Main.AnalyzedClass.from(rr.getTarget(), target);
//                        analyzedClass.containsFields(rr.getFields().toArray(new String[rr.getFields().size()]));
////                    analyzedClass.containsMethod()
//                    });
//                    return false;
//                }).getOrElse(true);
//            })).getOrElse(false);
//        }
//    }
//        }


