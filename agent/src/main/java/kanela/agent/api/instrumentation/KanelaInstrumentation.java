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

import io.vavr.Function0;
import io.vavr.Function1;
import io.vavr.Function2;
import kanela.agent.api.advisor.AdvisorDescription;
import kanela.agent.api.instrumentation.bridge.BridgeDescription;
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
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.*;

public abstract class KanelaInstrumentation {
    private final ListBuilder<InstrumentationDescription> instrumentationDescriptions = ListBuilder.builder();

    protected final ElementMatcher.Junction<ByteCodeElement> notDeclaredByObject = not(isDeclaredBy(Object.class));
    protected final ElementMatcher.Junction<MethodDescription> notTakesArguments = not(takesArguments(0));

    private static Function0<ElementMatcher.Junction<TypeDescription>> defaultTypeMatcher =
            Function0.of(() -> not(isInterface()).and(not(isSynthetic()))).memoized();

    public List<TypeTransformation> collectTransformations(ModuleConfiguration moduleConfiguration, Instrumentation instrumentation) {
        return instrumentationDescriptions
                .build()
                .map(instrumentationDescription -> buildTransformations(instrumentationDescription, moduleConfiguration, instrumentation))
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
            val advisorClasses = advisors.stream().map(AdvisorDescription::getInterceptorClass).collect(Collectors.toList());

            val allClasses = new ArrayList<Class<?>>();
            allClasses.addAll(bridgeClasses);
            allClasses.addAll(mixinClasses);
            allClasses.addAll(advisorClasses);

            BootstrapInjector.inject(moduleConfiguration.getTempDir(), instrumentation, allClasses);
        }

        return TypeTransformation.of(
                instrumentationDescription.getElementMatcher(),
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

    public void forTargetType(Supplier<String> f, Function1<InstrumentationDescription.Builder, InstrumentationDescription> instrumentationFunction) {
        val builder = new InstrumentationDescription.Builder();
        builder.addElementMatcher(() -> failSafe(named(f.get())));
        instrumentationDescriptions.add(instrumentationFunction.apply(builder));
    }

    public void forSubtypeOf(Supplier<String> f, Function1<InstrumentationDescription.Builder, InstrumentationDescription> instrumentationFunction) {
        val builder = new InstrumentationDescription.Builder();
        builder.addElementMatcher(() -> defaultTypeMatcher.apply().and(failSafe(hasSuperType(named(f.get())))));
        instrumentationDescriptions.add(instrumentationFunction.apply(builder));
    }

    public void forTypesAnnnotatedWith(Supplier<String> f, Function1<InstrumentationDescription.Builder, InstrumentationDescription> instrumentationFunction) {
        val builder = new InstrumentationDescription.Builder();
        builder.addElementMatcher(() -> defaultTypeMatcher.apply().and(failSafe(isAnnotatedWith(named(f.get())))));
        instrumentationDescriptions.add(instrumentationFunction.apply(builder));
    }

    public void forTypesWithMethodsAnnotatedWith(Supplier<String> f, Function2<InstrumentationDescription.Builder, ElementMatcher.Junction<MethodDescription>, InstrumentationDescription> instrumentationFunction) {
        val builder = new InstrumentationDescription.Builder();
        final ElementMatcher.Junction<MethodDescription>  methodMatcher = isAnnotatedWith(named(f.get()));
        builder.addElementMatcher(() -> defaultTypeMatcher.apply().and(failSafe(hasSuperType(declaresMethod(methodMatcher)))));
        instrumentationDescriptions.add(instrumentationFunction.apply(builder, methodMatcher));
    }

    public ElementMatcher.Junction<MethodDescription> method(String name){ return named(name);}

    public ElementMatcher.Junction<MethodDescription> isConstructor() { return ElementMatchers.isConstructor();}

    public ElementMatcher.Junction<MethodDescription> isAbstract() { return ElementMatchers.isAbstract();}

    public ElementMatcher.Junction<MethodDescription> takesArguments(Integer quantity) { return ElementMatchers.takesArguments(quantity);}

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
}
