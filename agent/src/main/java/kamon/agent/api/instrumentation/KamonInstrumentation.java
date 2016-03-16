package kamon.agent.api.instrumentation;

import javaslang.Function1;
import javaslang.Function3;
import javaslang.control.Option;
import kamon.agent.api.advisor.AdvisorDescription;
import kamon.agent.api.instrumentation.listener.InstrumentationListener;
import kamon.agent.api.instrumentation.mixin.MixinClassVisitorWrapper;
import kamon.agent.api.instrumentation.mixin.MixinDescription;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Identified;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper.ForDeclaredMethods;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static net.bytebuddy.matcher.ElementMatchers.*;

public abstract class KamonInstrumentation {
    private List<InstrumentationDescription> instrumentationDescriptions = new ArrayList<>();

    protected final TypePool typePool = TypePool.Default.ofClassPath();
    protected final ElementMatcher.Junction<ByteCodeElement> NotDeclaredByObject = not(isDeclaredBy(Object.class));
    protected final ElementMatcher.Junction<MethodDescription> TakesArguments = not(takesArguments(0));

    public void register(Instrumentation instrumentation) {
        final AgentBuilder agentBuilder = new AgentBuilder.Default().with(new InstrumentationListener());

        instrumentationDescriptions.forEach(instrumentationDescription -> {
            Identified a = agentBuilder.type(instrumentationDescription.getElementMatcher().getOrElseThrow(() -> new RuntimeException("There must be an element selected by elementMatcher")));

            instrumentationDescription.getMixins().forEach(mixin ->
                    a.transform((builder, typeDescription, classLoader) -> builder.visit(new MixinClassVisitorWrapper(mixin))).installOn(instrumentation));

            instrumentationDescription.getInterceptors().forEach(interceptor -> a.transform((builder, typeDescription, classLoader) ->
                    builder.visit(new ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(interceptor.getMethodMatcher(), Advice.to(interceptor.getInterceptorClass())))).installOn(instrumentation));

            instrumentationDescription.getTransformers().forEach(transformer -> a.transform(transformer).installOn(instrumentation));
        });

    }

//    public void forTypes(Supplier<ElementMatcher<? super TypeDescription>> f) { instrumentationDescription.elementMatcher = Option.of(f.get());}

//    public void forType(Supplier<ElementMatcher<? super TypeDescription>> f) {forTypes(f);}

//    public void forTargetType(Supplier<String> f) {
//        forType((() -> named(f.get())));
//    }

    public void forTargetType2(Supplier<String> f, Function1<InstrumentationDescription.Builder, InstrumentationDescription> instrumentationFunction) {
        InstrumentationDescription.Builder builder = new InstrumentationDescription.Builder();
        builder.addElementMatcher(() -> named(f.get()));
        instrumentationDescriptions.add(instrumentationFunction.apply(builder));
    }

    //    public void forSubtypeOf(Supplier<String> f){
//        forType(() -> isSubTypeOf(typePool.describe(f.get()).resolve()).and(not(isInterface())));
//    }
    public void forSubtypeOf2(Supplier<String> f, Function1<InstrumentationDescription.Builder, InstrumentationDescription> instrumentationFunction) {
        InstrumentationDescription.Builder builder = new InstrumentationDescription.Builder();
        builder.addElementMatcher(() -> isSubTypeOf(typePool.describe(f.get()).resolve()).and(not(isInterface())));
        instrumentationDescriptions.add(instrumentationFunction.apply(builder));
    }

//    public void addMixin(Supplier<Class<?>> f) {mixins.add(MixinDescription.of(elementMatcher.get(),f.get()));}
//
//    public void addAdvisor(ElementMatcher.Junction<MethodDescription> methodDescription , Supplier<Class<?>> classSupplier) {
//        interceptors.add(new AdvisorDescription(methodDescription, classSupplier.get()));
//    }
}
