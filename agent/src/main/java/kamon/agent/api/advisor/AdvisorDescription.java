/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

package kamon.agent.api.advisor;

import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

@Value(staticConstructor = "of")
public class AdvisorDescription {
    ElementMatcher<? super MethodDescription.InDefinedShape> methodMatcher;
    Class<?> interceptorClass;

    public AgentBuilder.Transformer makeTransformer() {
        return (builder, typeDescription, classLoader) -> builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().method(this.getMethodMatcher(), Advice.to(this.getInterceptorClass())));
    }
}
