package kamon.agent.api.instrumentation;

import javaslang.Function1;
import kamon.agent.api.advisor.AdvisorDescription;
import kamon.agent.api.instrumentation.mixin.MixinDescription;
import net.bytebuddy.agent.builder.AgentBuilder;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.*;

import lombok.EqualsAndHashCode;
import lombok.Value;

public abstract class KamonInstrumentation {
    private final List<InstrumentationDescription> instrumentationDescriptions = new ArrayList<>();

    private final TypePool typePool = TypePool.Default.ofClassPath();
    protected final ElementMatcher.Junction<ByteCodeElement> NotDeclaredByObject = not(isDeclaredBy(Object.class));
    protected final ElementMatcher.Junction<MethodDescription> TakesArguments = not(takesArguments(0));

    public List<TypeTransformation> collectTransformations() {
        return instrumentationDescriptions.stream()
                .map(this::makeTransformations)
                .collect(Collectors.toList());
    }

    private TypeTransformation makeTransformations(InstrumentationDescription instrumentationDescription) {
        final Set<AgentBuilder.Transformer> mixins = toTransformers(instrumentationDescription.mixins(), MixinDescription::makeTransformer);
        final Set<AgentBuilder.Transformer> advisors = toTransformers(instrumentationDescription.interceptors(), AdvisorDescription::makeTransformer);
        final Set<AgentBuilder.Transformer> transformers = toTransformers(instrumentationDescription.transformers(), Function.identity());

        return TypeTransformation.of(instrumentationDescription.elementMatcher(), mixins, advisors, transformers);
    }

    private <T> Set<AgentBuilder.Transformer> toTransformers(List<T> transformerList, Function<T, AgentBuilder.Transformer> f) {
        return transformerList.stream()
                .map(f)
                .collect(Collectors.toSet());
    }

    public void forTargetType(Supplier<String> f, Function1<InstrumentationDescription.Builder, InstrumentationDescription> instrumentationFunction) {
        InstrumentationDescription.Builder builder = new InstrumentationDescription.Builder();
        builder.addElementMatcher(() -> defaultTypeMatcher().and(named(f.get())));
        instrumentationDescriptions.add(instrumentationFunction.apply(builder));
    }

    public void forSubtypeOf(Supplier<String> f, Function1<InstrumentationDescription.Builder, InstrumentationDescription> instrumentationFunction) {
        InstrumentationDescription.Builder builder = new InstrumentationDescription.Builder();
        builder.addElementMatcher(() -> defaultTypeMatcher().and(isSubTypeOf(typePool.describe(f.get()).resolve())));
        instrumentationDescriptions.add(instrumentationFunction.apply(builder));
    }

    private ElementMatcher.Junction<TypeDescription> defaultTypeMatcher() {
        return  failSafe(not(isInterface()).and(not(isSynthetic())));
    }

    public boolean isActive() {
        return true;
    }

    public int order() {
        return 1;
    }

    @Value
    @EqualsAndHashCode(callSuper=false)
    public static class NoOp extends KamonInstrumentation {
        Throwable cause;

        @Override
        public boolean isActive() {
            return false;
        }
    }
}
