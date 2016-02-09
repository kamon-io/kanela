package kamon.agent.api.instrumentation;

import javaslang.Function2;
import javaslang.collection.List;
import javaslang.control.Option;
import kamon.agent.api.instrumentation.mixin.MixinDescription;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;

import java.lang.instrument.Instrumentation;
import java.util.function.Supplier;

public abstract class KamonInstrumentation {
    private Option<ElementMatcher<? super TypeDescription>> elementMatcher = null;
    private List<MixinDescription> mixins = null;
    private List<AgentBuilder.Transformer> transformers = null;

    protected static final TypePool typePool = null;
    protected static final ElementMatcher.Junction<ByteCodeElement> NotDeclaredByObject = null;
    protected static final ElementMatcher.Junction<MethodDescription> NotTakesArguments = null;

    public void register(Instrumentation instrumentation) {
        throw new RuntimeException("This instrumentation must be redefined by the Kamon Agent");
    }

    private AgentBuilder.Transformer withTransformer(Function2<DynamicType.Builder, TypeDescription, DynamicType.Builder> f) { return null; }

    public void addTransformation(Function2<DynamicType.Builder, TypeDescription, DynamicType.Builder> f) { }

    public void forTypes(Supplier<ElementMatcher<? super TypeDescription>> f) { }
    public void forType(Supplier<ElementMatcher<? super TypeDescription>> f) { }
    public void forTargetType(Supplier<String> f) {
        throw new RuntimeException("This instrumentation must be redefined by the Kamon Agent");
    }
    public void forSubtypeOf(Supplier<String> f){ }
    public void addMixin(Supplier<Class<?>> f) { }

}
