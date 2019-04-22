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

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

import io.vavr.Function0;
import kanela.agent.api.advisor.AdvisorDescription;
import kanela.agent.api.instrumentation.bridge.BridgeDescription;
import kanela.agent.api.instrumentation.classloader.ClassLoaderRefiner;
import kanela.agent.api.instrumentation.classloader.ClassRefiner;
import kanela.agent.api.instrumentation.legacy.LegacySupportTransformer;
import kanela.agent.api.instrumentation.mixin.MixinDescription;
import kanela.agent.util.BootstrapInjector;
import kanela.agent.util.ListBuilder;
import kanela.agent.util.conf.KanelaConfiguration.ModuleConfiguration;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.*;

public abstract class InstrumentationBuilder {
    private final ListBuilder<Target> targets = ListBuilder.builder();

    protected final ElementMatcher.Junction<ByteCodeElement> notDeclaredByObject = not(isDeclaredBy(Object.class));
    protected final ElementMatcher.Junction<MethodDescription> notTakesArguments = not(takesArguments(0));

    private static Function0<ElementMatcher.Junction<TypeDescription>> defaultTypeMatcher =
            Function0.of(() -> not(isInterface()).and(not(isSynthetic()))).memoized();

    public List<TypeTransformation> collectTransformations(ModuleConfiguration moduleConfiguration, Instrumentation instrumentation) {
        return targets
                .build()
                .map(t -> buildTransformations(t.instrumentationDescription(), moduleConfiguration, instrumentation))
                .toJavaList();
    }

    private TypeTransformation buildTransformations(InstrumentationDescription instrumentationDescription, ModuleConfiguration moduleConfiguration, Instrumentation instrumentation) {

        val bridges = instrumentationDescription.getBridges();
        val mixins = instrumentationDescription.getMixins();
        val advisors = instrumentationDescription.getAdvisors();
        val transformers  = instrumentationDescription.getTransformers();

        if (moduleConfiguration.shouldSupportLegacyBytecode()) {
            transformers.add(LegacySupportTransformer.Instance);
        }

        if (moduleConfiguration.shouldInjectInBootstrap()) {
            val bridgeClasses = bridges.stream().map(BridgeDescription::getIface).collect(Collectors.toList());
            val mixinClasses = mixins.stream().flatMap(mixinDescription -> mixinDescription.getInterfaces().stream()).collect(Collectors.toList());
            val advisorClasses = advisors.stream().map(AdvisorDescription::getAdvisorClass).collect(Collectors.toList());

            val allClasses = new ArrayList<Class<?>>();
            allClasses.addAll(bridgeClasses);
            allClasses.addAll(mixinClasses);
            allClasses.addAll(advisorClasses);

            BootstrapInjector.inject(moduleConfiguration.getTempDir(), instrumentation, allClasses);
        }

        return TypeTransformation.of(
                this.getClass().getName(),
                instrumentationDescription.getElementMatcher(),
                instrumentationDescription.getClassLoaderRefiner(),
                collect(bridges, BridgeDescription::makeTransformer),
                collect(mixins, MixinDescription::makeTransformer),
                collect(advisors, AdvisorDescription::makeTransformer),
                collect(transformers, Function.identity()));
    }

    private <T> List<AgentBuilder.Transformer> collect(List<T> transformerList, Function<T, AgentBuilder.Transformer> f) {
        return transformerList.stream()
                .map(f)
                .collect(Collectors.toList());
    }

    public Target onType(String typeName) {
        val builder = new InstrumentationDescription.Builder();
        val target = new Target(builder);
        builder.addElementMatcher(() -> failSafe(named(typeName)));
        targets.add(target);
        return target;
    }

    public Target onTypes(String... typeName) {
        val builder = new InstrumentationDescription.Builder();
        val target = new Target(builder);
        builder.addElementMatcher(() -> failSafe(anyTypes(typeName)));
        targets.add(target);
        return target;
    }

    public Target onSubTypesOf(String... typeName) {
        val builder = new InstrumentationDescription.Builder();
        val target = new Target(builder);
        builder.addElementMatcher(() -> defaultTypeMatcher.apply().and(failSafe(hasSuperType(anyTypes(typeName)))));
        targets.add(target);
        return target;
    }

    public Target onTypesAnnotatedWith(String annotationName) {
        val builder = new InstrumentationDescription.Builder();
        val target = new Target(builder);
        builder.addElementMatcher(() -> defaultTypeMatcher.apply().and(failSafe(isAnnotatedWith(named(annotationName)))));
        targets.add(target);
        return target;
    }

    public Target onTypesWithMethodsAnnotatedWith(String annotationName) {
        val builder = new InstrumentationDescription.Builder();
        val target = new Target(builder);
        final ElementMatcher.Junction<MethodDescription>  methodMatcher = isAnnotatedWith(named(annotationName));
        builder.addElementMatcher(() -> defaultTypeMatcher.apply().and(failSafe(hasSuperType(declaresMethod(methodMatcher)))));
        targets.add(target);
        return target;
    }

    public Target onTypesMatching(ElementMatcher<? super TypeDescription> typeMatcher) {
        val builder = new InstrumentationDescription.Builder();
        val target = new Target(builder);
        builder.addElementMatcher(() -> defaultTypeMatcher.apply().and(failSafe(typeMatcher)));
        targets.add(target);
        return target;
    }

    public ElementMatcher.Junction<MethodDescription> method(String name){ return named(name);}

    public ElementMatcher.Junction<MethodDescription> isConstructor() { return ElementMatchers.isConstructor();}

    public ElementMatcher.Junction<MethodDescription> isAbstract() { return ElementMatchers.isAbstract();}

    public Junction<? super TypeDescription> anyTypes(String... names) { return io.vavr.collection.List.of(names).map(ElementMatchers::named).reduce(ElementMatcher.Junction::or); }

    public ElementMatcher.Junction<MethodDescription> takesArguments(Integer quantity) { return ElementMatchers.takesArguments(quantity);}

    public ElementMatcher.Junction<MethodDescription> takesOneArgumentOf(String type) { return ElementMatchers.takesArgument(0, named(type));}

    public ElementMatcher.Junction<MethodDescription> withArgument(Integer index, Class<?> type) { return ElementMatchers.takesArgument(index, type);}

    public ElementMatcher.Junction<MethodDescription> withArgument(Class<?> type) { return withArgument(0, type);}

    public ElementMatcher.Junction<MethodDescription> anyMethods(String... names) { return io.vavr.collection.List.of(names).map(this::method).reduce(ElementMatcher.Junction::or); }

    public ElementMatcher.Junction<MethodDescription> withReturnTypes(Class<?>... types) { return io.vavr.collection.List.of(types).map(ElementMatchers::returns).reduce(ElementMatcher.Junction::or);}

    public ElementMatcher.Junction<MethodDescription> methodAnnotatedWith(String annotation) { return ElementMatchers.isAnnotatedWith(named(annotation)); }

    public ElementMatcher.Junction<MethodDescription> methodAnnotatedWith(Class<? extends Annotation> annotation) { return ElementMatchers.isAnnotatedWith(annotation); }

    public boolean isEnabled(ModuleConfiguration moduleConfiguration) {
        return moduleConfiguration.isEnabled();
    }

    public int order() {
        return 1;
    }

    public ClassRefiner.Builder classIsPresent(String className) {
        return ClassRefiner.builder().mustContains(className);
    }

    public static class Target {
        private final InstrumentationDescription.Builder builder;

        Target(InstrumentationDescription.Builder builder) {
            this.builder = builder;
        }

        public Target mixin(Class<?> implementation) {
            builder.withMixin(() -> implementation);
            return this;
        }

        public Target bridge(Class<?> implementation) {
            builder.withBridge(() -> implementation);
            return this;
        }

        public Target advise(ElementMatcher.Junction<MethodDescription> method, Class<?> implementation) {
            builder.withAdvisorFor(method, () -> implementation);
            return this;
        }

        public Target intercept(ElementMatcher.Junction<MethodDescription> method, Class<?> implementation) {
            builder.withInterceptorFor(method, () -> implementation);
            return this;
        }

        public Target intercept(ElementMatcher.Junction<MethodDescription> method, Object implementation) {
            builder.withInterceptorFor(method, implementation);
            return this;
        }

        public Target when(ClassRefiner.Builder... refinerBuilders) {
            val refiners = io.vavr.collection.List.of(refinerBuilders).map(b -> b.build()).toJavaArray(ClassRefiner.class);
            builder.withClassLoaderRefiner(() -> ClassLoaderRefiner.from(refiners));
            return this;
        }

        public Target when(ClassRefiner... refiners) {
            builder.withClassLoaderRefiner(() -> ClassLoaderRefiner.from(refiners));
            return this;
        }

        private InstrumentationDescription instrumentationDescription() {
            return builder.build();
        }
    }
}
