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

import io.vavr.Function4;
import io.vavr.control.Option;
import kanela.agent.api.advisor.AdvisorDescription;
import kanela.agent.api.instrumentation.bridge.BridgeDescription;
import kanela.agent.api.instrumentation.classloader.ClassLoaderRefiner;
import kanela.agent.api.instrumentation.mixin.MixinDescription;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static net.bytebuddy.matcher.ElementMatchers.*;

@Value
public class InstrumentationDescription {
    Option<ElementMatcher<? super TypeDescription>> elementMatcher;
    Option<ClassLoaderRefiner> classLoaderRefiner;
    List<MixinDescription> mixins;
    List<BridgeDescription> bridges;
    List<AdvisorDescription> advisors;
    List<AgentBuilder.Transformer> transformers;

    private InstrumentationDescription(Builder builder) {
        this.elementMatcher  = builder.elementMatcher;
        this.classLoaderRefiner = builder.classLoaderRefiner;
        this.mixins = builder.mixins;
        this.bridges = builder.bridges;
        this.advisors = builder.advisors;
        this.transformers = builder.transformers;
    }

    public static class Builder {
        private Option<ElementMatcher<? super TypeDescription>> elementMatcher;
        private Option<ClassLoaderRefiner> classLoaderRefiner = Option.none();
        private final List<MixinDescription> mixins =  new ArrayList<>();
        private final List<BridgeDescription> bridges =  new ArrayList<>();
        private final List<AdvisorDescription> advisors = new ArrayList<>();
        private final List<AgentBuilder.Transformer> transformers = new ArrayList<>();


        Builder addElementMatcher(Supplier<ElementMatcher<? super TypeDescription>> f) {
            elementMatcher = Option.of(f.get());
            return this;
        }

        public Builder withMixin(Supplier<Class<?>> clazz) {
            mixins.add(MixinDescription.of(clazz.get()));
            return this;
        }

        public Builder withBridge(Supplier<Class<?>> clazz) {
            bridges.add(BridgeDescription.of(clazz.get()));
            return this;
        }

        public Builder withClassLoaderRefiner(Supplier<ClassLoaderRefiner> clazz) {
            classLoaderRefiner = Option.of(clazz.get());
            return this;
        }

        public Builder withAdvisorFor(ElementMatcher.Junction<MethodDescription> methodDescription , Supplier<Class<?>> classSupplier) {
            advisors.add(AdvisorDescription.of(methodDescription.and(defaultMethodElementMatcher()), classSupplier.get()));
            return this;
        }

        public Builder withInterceptorFor(ElementMatcher.Junction<MethodDescription> method, Supplier<Class<?>> delegate) {
            withTransformation((builder, typeDescription, classLoader, javaModule) -> builder.method(method).intercept(MethodDelegation.to(delegate.get())));
            return this;
        }

        public Builder withInterceptorFor(ElementMatcher.Junction<MethodDescription> method, Object delegate) {
            withTransformation((builder, typeDescription, classLoader, javaModule) -> builder.method(method).intercept(MethodDelegation.to(delegate)));
            return this;
        }

        public Builder withTransformation(Function4<DynamicType.Builder, TypeDescription, ClassLoader, JavaModule, DynamicType.Builder> f) {
            transformers.add(withTransformer(f));
            return this;
        }

        private AgentBuilder.Transformer withTransformer(Function4<DynamicType.Builder, TypeDescription, ClassLoader, JavaModule, DynamicType.Builder> f) { return f::apply; }

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
