package kamon.agent.builder;

import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@Value(staticConstructor = "of")
class TransformerDescription {
    ElementMatcher<? super TypeDescription> elementMatcher;
    AgentBuilder.Transformer transformer;
}
