package kamon.agent.api.instrumentation;

import javaslang.Function3;
import javaslang.control.Option;
import kamon.agent.api.advisor.AdvisorDescription;
import kamon.agent.api.instrumentation.mixin.MixinDescription;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class InstrumentationDescription {
    private final Option<ElementMatcher<? super TypeDescription>> elementMatcher;
    private final List<MixinDescription> mixins;
    private final List<AdvisorDescription> interceptors;
    private final List<AgentBuilder.Transformer> transformers;

    private InstrumentationDescription(Builder builder) {
        this.elementMatcher  = builder.elementMatcher;
        this.mixins = builder.mixins;
        this.interceptors = builder.interceptors;
        this.transformers = builder.transformers;
    }

    Option<ElementMatcher<? super TypeDescription>> elementMatcher() {
        return elementMatcher;
    }

    List<MixinDescription> mixins() {
        return mixins;
    }

    List<AdvisorDescription> interceptors() {
        return interceptors;
    }

    List<AgentBuilder.Transformer> transformers() {
        return transformers;
    }

    public static class Builder {
        private Option<ElementMatcher<? super TypeDescription>> elementMatcher;
        private List<MixinDescription> mixins =  new ArrayList<>();
        private List<AdvisorDescription> interceptors = new ArrayList<>();
        private List<AgentBuilder.Transformer> transformers = new ArrayList<>();


        Builder addElementMatcher(Supplier<ElementMatcher<? super TypeDescription>> f) {
            elementMatcher = Option.of(f.get());
            return this;
        }

        public Builder withMixin(Supplier<Class<?>> clazz) {
            mixins.add(MixinDescription.of(elementMatcher.get(),clazz.get()));
            return this;
        }

        public Builder withAdvisorFor(ElementMatcher.Junction<MethodDescription> methodDescription , Supplier<Class<?>> classSupplier) {
            interceptors.add(new AdvisorDescription(methodDescription.and(defaultMethodElementMatcher()), classSupplier.get()));
            return this;
        }

        public Builder withTransformation(Function3<DynamicType.Builder, TypeDescription, ClassLoader, DynamicType.Builder> f) {
            transformers.add(withTransformer(f));
            return this;
        }

        private AgentBuilder.Transformer withTransformer(Function3<net.bytebuddy.dynamic.DynamicType.Builder, TypeDescription, ClassLoader, net.bytebuddy.dynamic.DynamicType.Builder> f) { return f::apply; }

        public InstrumentationDescription build() {
            return new InstrumentationDescription(this);
        }

        private ElementMatcher.Junction<MethodDescription> defaultMethodElementMatcher() {
            return not(isAbstract())
                    .and(not(isNative()))
                    .and(not(isSynthetic()))
                    .and(not(isTypeInitializer()));
        }
    }
}
