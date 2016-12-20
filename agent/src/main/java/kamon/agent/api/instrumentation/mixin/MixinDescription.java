package kamon.agent.api.instrumentation.mixin;

import javaslang.control.Option;
import javaslang.control.Try;
import kamon.agent.api.instrumentation.Initializer;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher;
import utils.AgentApiUtils;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class MixinDescription {

    Type implementation;
    Set<String> interfaces;
    byte[] bytes;
    Option<String> mixinInit;
    ElementMatcher targetTypes;

    private MixinDescription(Type implementation,
                             Set<String> interfaces,
                             byte[] bytes,
                             Option<String> mixinInit,
                             ElementMatcher targetTypes) {
        this.implementation = implementation;
        this.interfaces = interfaces;
        this.bytes = bytes;
        this.mixinInit = mixinInit;
        this.targetTypes = targetTypes;
    }

    public static MixinDescription of(ElementMatcher targetTypes, Class<?> clazz) {
        final Type implementation = Type.getType(clazz);
        final Set<String> interfaces = Arrays.stream(clazz.getInterfaces()).map(name -> Type.getType(name).getInternalName()).collect(Collectors.toSet());
        final Option<String> mixinInit = Option.ofOptional(Arrays.stream(clazz.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(Initializer.class)).findFirst().map(Method::getName));
        return new MixinDescription(implementation, interfaces, getBytesFrom(clazz), mixinInit, targetTypes);
    }

    private static byte[] getBytesFrom(Class<?> implementation) {
        final ClassLoader loader = implementation.getClassLoader();
        final String resourceName = implementation.getName().replace('.','/') + ".class";
        final InputStream stream = loader.getResourceAsStream(resourceName);
        return Try.of(() -> AgentApiUtils.streamToByteArray(stream)).getOrElseThrow(() -> new RuntimeException(""));
    }

    public AgentBuilder.Transformer makeTransformer() {
        return (builder, typeDescription, classLoader) -> builder.visit(MixinClassVisitorWrapper.of(this));
    }
}