package kamon.api.instrumentation;

import javaslang.Function2;
import javaslang.collection.List;
import javaslang.control.Option;
import kamon.api.instrumentation.listener.InstrumentationListener1;
import kamon.instrumentation.mixin.MixinDescription;
import kamon.instrumentation.mixin.MixinClassVisitorWrapper;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Identified;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;

import java.lang.instrument.Instrumentation;
import java.util.function.Supplier;

import static net.bytebuddy.matcher.ElementMatchers.*;

public abstract class KamonInstrumentation {
    private Option<ElementMatcher> elementMatcher = Option.none();
    private List<MixinDescription> mixins = List.empty();
    private List<Transformer> transformers = List.empty();

    protected static final TypePool typePool = TypePool.Default.ofClassPath();
    protected static final ElementMatcher.Junction NotDeclaredByObject = not(isDeclaredBy(Object.class));
    protected static final ElementMatcher.Junction NotTakesArguments = not(takesArguments(0));

    public void register(Instrumentation instrumentation) {
        Identified agentBuilder = new AgentBuilder.Default()
                .withListener(new InstrumentationListener1())
                .type(elementMatcher.getOrElseThrow(() -> new RuntimeException("")));

        mixins.forEach(mixin ->
                agentBuilder
                        .transform((builder, typeDescription) -> builder.visit(new MixinClassVisitorWrapper(mixin)))
                        .installOn(instrumentation));

        transformers.forEach(transformer -> agentBuilder.transform(transformer).installOn(instrumentation));
    }

    private Transformer withTransformer(Function2<Builder, TypeDescription, Builder> f) {return f::apply;}

    public void addTransformation(Function2<Builder, TypeDescription, Builder> f) {
     transformers.append(withTransformer(f));
    }

    public void forTypes(Supplier<ElementMatcher> f) { elementMatcher = Option.of(f.get());}
    public void forType(Supplier<ElementMatcher> f) {forTypes(f);}
    public void forTargetType(Supplier<String> f) {forType((() -> named(f.get())));}
    public void forSubtypeOf(Supplier<String> f){forType(() -> isSubTypeOf(typePool.describe(f.get()).resolve()).and(not(isInterface())));}
    public void addMixin(Supplier<Class<?>> f) {mixins.append(MixinDescription.of(elementMatcher.get(),f.get()));}

 }
