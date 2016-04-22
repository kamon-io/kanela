package kamon.agent.api.instrumentation;

import javaslang.Function1;
import kamon.agent.api.instrumentation.listener.InstrumentationListener;
import kamon.agent.api.instrumentation.mixin.MixinClassVisitorWrapper;
import kamon.agent.api.instrumentation.mixin.MixinDescription;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Identified;
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
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.bytebuddy.matcher.ElementMatchers.*;

public abstract class KamonInstrumentation {
    private final List<InstrumentationDescription> instrumentationDescriptions = new ArrayList<>();

    private final TypePool typePool = TypePool.Default.ofClassPath();
    protected final ElementMatcher.Junction<ByteCodeElement> NotDeclaredByObject = not(isDeclaredBy(Object.class));
    protected final ElementMatcher.Junction<MethodDescription> TakesArguments = not(takesArguments(0));

    public void register(Instrumentation instrumentation) {
        final AgentBuilder agentBuilder = new AgentBuilder.Default()
                .disableClassFormatChanges()
//                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(new InstrumentationListener());
        instrumentationDescriptions.forEach((instrumentationDescription) -> installInstrumentations(agentBuilder, instrumentationDescription, instrumentation));
    }

    private void installInstrumentations(AgentBuilder agentBuilder, InstrumentationDescription instrumentationDescription, Instrumentation instrumentation) {
        final Identified identified = agentBuilder
                .ignore(nameMatches("sun..*"))
                .ignore(nameMatches("java..*"))
                .ignore(nameMatches("javax..*"))
                .ignore(nameMatches("kamon.agent..*"))
                .ignore(nameMatches("kamon.testkit..*"))
                .ignore(nameMatches("kamon.instrumentation..*"))
                .ignore(nameMatches("akka.testkit..*"))
                .ignore(nameMatches("org.scalatest..*"))
                .ignore(nameMatches("scala.collection..*"))
                .ignore(is(isSystemClassLoader()))
                .type(instrumentationDescription.elementMatcher().getOrElseThrow(() -> new RuntimeException("There must be an element selected by elementMatcher")));

        instrumentationDescription.mixins().forEach(mixin ->
                identified.transform((builder, typeDescription, classLoader) -> builder.visit(new MixinClassVisitorWrapper(mixin))).installOn(instrumentation));

        instrumentationDescription.interceptors().forEach(interceptor -> identified.transform((builder, typeDescription, classLoader) ->
                builder.visit(new ForDeclaredMethods().method(interceptor.getMethodMatcher(), Advice.to(interceptor.getInterceptorClass())))).installOn(instrumentation));

        instrumentationDescription.transformers().forEach(transformer -> identified.transform(transformer).installOn(instrumentation));
    }

    public void forTargetType(Supplier<String> f, Function1<InstrumentationDescription.Builder, InstrumentationDescription> instrumentationFunction) {
        InstrumentationDescription.Builder builder = new InstrumentationDescription.Builder();
        builder.addElementMatcher(() -> failSafe(named(f.get())));
        instrumentationDescriptions.add(instrumentationFunction.apply(builder));
    }

    public void forSubtypeOf(Supplier<String> f, Function1<InstrumentationDescription.Builder, InstrumentationDescription> instrumentationFunction) {
        InstrumentationDescription.Builder builder = new InstrumentationDescription.Builder();
        builder.addElementMatcher(() -> failSafe(isSubTypeOf(typePool.describe(f.get()).resolve()).and(not(isInterface()))));
        instrumentationDescriptions.add(instrumentationFunction.apply(builder));
    }


    //    public void forSubtypeOf(Supplier<String> f){
//        forType(() -> isSubTypeOf(typePool.describe(f.get()).resolve()).and(not(isInterface())));
//    }

    //    public void forTypes(Supplier<ElementMatcher<? super TypeDescription>> f) { instrumentationDescription.elementMatcher = Option.of(f.get());}

//    public void forType(Supplier<ElementMatcher<? super TypeDescription>> f) {forTypes(f);}

//    public void forTargetType(Supplier<String> f) {
//        forType((() -> named(f.get())));
//    }


}
