package kamon.agent.api.advisor;

import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

@Value
public class AdvisorDescription {
    ElementMatcher<? super MethodDescription.InDefinedShape> methodMatcher;
    Class<?> interceptorClass;


    public AgentBuilder.Transformer makeTransformer() {
        return (builder, typeDescription, classLoader) -> builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().method(this.getMethodMatcher(), Advice.to(this.getInterceptorClass())));
    }
}
