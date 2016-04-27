package kamon.agent.api.instrumentation;

import javaslang.collection.HashMap;
import javaslang.collection.HashSet;
import javaslang.control.Option;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class TypeTransformation {

    Option<ElementMatcher<? super TypeDescription>> elementMatcher;
    javaslang.collection.Set<AgentBuilder.Transformer> mixins;
    javaslang.collection.Set<AgentBuilder.Transformer> transformations;

    @SafeVarargs
    static TypeTransformation of(Option<ElementMatcher<? super TypeDescription>> elementMatcher, Set<AgentBuilder.Transformer> mixins, Set<AgentBuilder.Transformer>... transformers) {
        final Set<AgentBuilder.Transformer> transformations = Arrays.stream(transformers)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        return new TypeTransformation(elementMatcher, HashSet.ofAll(mixins),HashSet.ofAll(transformations));
    }

    public Boolean isActive() {
        return true;
    }
}
