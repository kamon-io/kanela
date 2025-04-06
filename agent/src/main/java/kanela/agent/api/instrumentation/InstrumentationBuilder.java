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

package kanela.agent.api.instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

public abstract class InstrumentationBuilder {
  private final List<Target.Builder> targetBuilders = new LinkedList<>();

  protected final ElementMatcher.Junction<ByteCodeElement> notDeclaredByObject =
      not(isDeclaredBy(Object.class));

  protected final ElementMatcher.Junction<MethodDescription> notTakesArguments =
      not(takesArguments(0));

  private static ElementMatcher.Junction<TypeDescription> defaultTypeMatcher =
      not(isInterface()).and(not(isSynthetic()));

  public List<Target> targets() {
    return targetBuilders.stream()
        .map(
            builder ->
                new Target(
                    builder.typeMatcher,
                    builder.bridgeInterfaces,
                    builder.mixinClasses,
                    builder.transformers,
                    builder.advisors,
                    builder.interceptors,
                    builder.classpathFilters))
        .collect(Collectors.toList());
  }

  public Target.Builder onType(String typeName) {
    Target.Builder target = new Target.Builder(named(typeName));
    targetBuilders.add(target);
    return target;
  }

  public Target.Builder onTypes(String... typeName) {
    Target.Builder target = new Target.Builder(anyTypes(typeName));
    targetBuilders.add(target);
    return target;
  }

  public Target.Builder onSubTypesOf(String... typeName) {
    Target.Builder target =
        new Target.Builder(defaultTypeMatcher.and(hasSuperType(anyTypes(typeName))));
    targetBuilders.add(target);
    return target;
  }

  public Target.Builder onTypesAnnotatedWith(String annotationName) {
    Target.Builder target =
        new Target.Builder(defaultTypeMatcher.and(isAnnotatedWith(named(annotationName))));
    targetBuilders.add(target);
    return target;
  }

  public Target.Builder onTypesWithMethodsAnnotatedWith(String annotationName) {
    final Junction<MethodDescription> methodMatcher = isAnnotatedWith(named(annotationName));
    Target.Builder target =
        new Target.Builder(defaultTypeMatcher.and(hasSuperType(declaresMethod(methodMatcher))));
    targetBuilders.add(target);
    return target;
  }

  public Target.Builder onTypesMatching(ElementMatcher<? super TypeDescription> typeMatcher) {
    Target.Builder target = new Target.Builder(defaultTypeMatcher.and(failSafe(typeMatcher)));
    targetBuilders.add(target);
    return target;
  }

  public ElementMatcher.Junction<MethodDescription> method(String name) {
    return named(name);
  }

  public ElementMatcher.Junction<MethodDescription> isConstructor() {
    return ElementMatchers.isConstructor();
  }

  public ElementMatcher.Junction<MethodDescription> isAbstract() {
    return ElementMatchers.isAbstract();
  }

  public ElementMatcher.Junction<TypeDescription> anyTypes(String... names) {
    return Arrays.stream(names)
        .map(name -> ElementMatchers.<TypeDescription>named(name))
        .reduce(Junction::or)
        .orElse(ElementMatchers.none());
  }

  public ElementMatcher.Junction<MethodDescription> takesArguments(Integer quantity) {
    return ElementMatchers.takesArguments(quantity);
  }

  public ElementMatcher.Junction<MethodDescription> takesOneArgumentOf(String type) {
    return ElementMatchers.takesArgument(0, named(type));
  }

  public ElementMatcher.Junction<MethodDescription> withArgument(Integer index, Class<?> type) {
    return ElementMatchers.takesArgument(index, type);
  }

  public ElementMatcher.Junction<MethodDescription> withArgument(Class<?> type) {
    return withArgument(0, type);
  }

  public ElementMatcher.Junction<MethodDescription> anyMethods(String... names) {
    return Arrays.stream(names)
        .map(this::method)
        .reduce(ElementMatcher.Junction::or)
        .orElse(ElementMatchers.none());
  }

  public ElementMatcher.Junction<MethodDescription> withReturnTypes(Class<?>... types) {
    return Arrays.stream(types)
        .map(ElementMatchers::returns)
        .reduce(ElementMatcher.Junction::or)
        .orElse(ElementMatchers.none());
  }

  public ElementMatcher.Junction<MethodDescription> methodAnnotatedWith(String annotation) {
    return ElementMatchers.isAnnotatedWith(named(annotation));
  }

  public ElementMatcher.Junction<MethodDescription> methodAnnotatedWith(
      Class<? extends Annotation> annotation) {
    return ElementMatchers.isAnnotatedWith(annotation);
  }

  public Target.ClasspathFilter classIsPresent(String className) {
    return new Target.ClasspathFilter(className, Collections.emptyList());
  }

  public static class Target {
    private final ElementMatcher<TypeDescription> typeMatcher;
    private final List<Class<?>> bridgeInterfaces;
    private final List<Class<?>> mixinClasses;
    private final List<AgentBuilder.Transformer> transformers;
    private final List<Advice> advisors;
    private final List<Interceptor> interceptors;
    private final List<ClasspathFilter> classpathFilters;

    Target(
        ElementMatcher<TypeDescription> typeMatcher,
        List<Class<?>> bridgeInterfaces,
        List<Class<?>> mixinClasses,
        List<AgentBuilder.Transformer> transformers,
        List<Advice> advisors,
        List<Interceptor> interceptors,
        List<ClasspathFilter> classpathFilters) {

      this.typeMatcher = typeMatcher;
      this.bridgeInterfaces = bridgeInterfaces;
      this.mixinClasses = mixinClasses;
      this.transformers = transformers;
      this.advisors = advisors;
      this.interceptors = interceptors;
      this.classpathFilters = classpathFilters;
    }

    public ElementMatcher<TypeDescription> typeMatcher() {
      return typeMatcher;
    }

    public List<Class<?>> bridgeInterfaces() {
      return bridgeInterfaces;
    }

    public List<Class<?>> mixinClasses() {
      return mixinClasses;
    }

    public List<AgentBuilder.Transformer> transformers() {
      return transformers;
    }

    public List<Advice> advisors() {
      return advisors;
    }

    public List<Interceptor> interceptors() {
      return interceptors;
    }

    public List<ClasspathFilter> classpathFilters() {
      return classpathFilters;
    }

    public static class Advice {
      private final ElementMatcher<MethodDescription> method;
      private final String implementationClassName;

      Advice(ElementMatcher<MethodDescription> method, String implementationClassName) {

        this.method = method;
        this.implementationClassName = implementationClassName;
      }

      public ElementMatcher<MethodDescription> method() {
        return method;
      }

      public String implementationClassName() {
        return implementationClassName;
      }
    }

    public static class Interceptor {
      private final ElementMatcher<MethodDescription> method;
      private final Object eitherObject;
      private final Class<?> orImplementation;

      Interceptor(
          ElementMatcher<MethodDescription> method,
          Object eitherObject,
          Class<?> orImplementation) {

        this.method = method;
        this.eitherObject = eitherObject;
        this.orImplementation = orImplementation;
      }

      public ElementMatcher<MethodDescription> method() {
        return method;
      }

      public Object eitherObject() {
        return eitherObject;
      }

      public Class<?> orImplementation() {
        return orImplementation;
      }
    }

    public static class ClasspathFilter {
      private final String className;
      private final List<String> expectedMethodNames;

      public ClasspathFilter(String className, List<String> expectedMethodNames) {
        this.className = className;
        this.expectedMethodNames = expectedMethodNames;
      }

      public String className() {
        return className;
      }

      public List<String> expectedMethodNames() {
        return expectedMethodNames;
      }

      public ClasspathFilter withExpectedMethodNames(String... methodNames) {
        return new ClasspathFilter(className, List.of(methodNames));
      }
    }

    public static class Builder {
      private final ElementMatcher<TypeDescription> typeMatcher;
      private final List<Class<?>> bridgeInterfaces = new LinkedList<>();
      private final List<Class<?>> mixinClasses = new LinkedList<>();
      private final List<AgentBuilder.Transformer> transformers = new LinkedList<>();
      private final List<Advice> advisors = new LinkedList<>();
      private final List<Interceptor> interceptors = new LinkedList<>();
      private final List<ClasspathFilter> classpathFilters = new LinkedList<>();

      public Builder(ElementMatcher<TypeDescription> typeMatcher) {
        this.typeMatcher = typeMatcher;
      }

      public Target.Builder mixin(Class<?> implementation) {
        mixinClasses.add(implementation);
        return this;
      }

      public Target.Builder bridge(Class<?> implementation) {
        bridgeInterfaces.add(implementation);
        return this;
      }

      public Target.Builder advise(Junction<MethodDescription> method, Class<?> implementation) {
        advisors.add(new Advice(method, implementation.getName()));
        return this;
      }

      public Target.Builder advise(Junction<MethodDescription> method, String implementation) {
        advisors.add(new Advice(method, implementation));
        return this;
      }

      public Target.Builder intercept(Junction<MethodDescription> method, Class<?> implementation) {
        interceptors.add(new Interceptor(method, null, implementation));
        return this;
      }

      public Target.Builder intercept(Junction<MethodDescription> method, Object implementation) {
        interceptors.add(new Interceptor(method, implementation, null));
        return this;
      }

      public Target.Builder when(ClasspathFilter filter) {
        classpathFilters.add(filter);
        return this;
      }
    }
  }
}
