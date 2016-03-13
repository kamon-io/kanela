package kamon.agent.api.advisor;

import lombok.Value;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

@Value
public class AdvisorDescription {
    ElementMatcher<? super MethodDescription.InDefinedShape> methodMatcher;
    Class<?> interceptorClass;
}
