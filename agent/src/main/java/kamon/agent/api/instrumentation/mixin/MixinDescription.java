package kamon.agent.api.instrumentation.mixin;

import javaslang.control.Option;
import javaslang.control.Try;
import kamon.agent.api.instrumentation.initializer;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher;
import utils.AgentApiUtils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class MixinDescription {

    private final Type implementation;
    private final Set<String> interfaces;
    private final byte[] bytes;
    private final Option<String> mixinInit;
    private final ElementMatcher targetTypes;

    public MixinDescription(Type implementation,
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
        Type implementation = Type.getType(clazz);
        Set<String> interfaces = Arrays.stream(clazz.getInterfaces()).map(name -> Type.getType(name).getInternalName()).collect(Collectors.toSet());
        Option<String> mixinInit = Option.of(Arrays.stream(clazz.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(initializer.class)).findFirst().get().getName());
        return new MixinDescription(implementation, interfaces, getBytesFrom(clazz), mixinInit, targetTypes);
    }

    private static byte[] getBytesFrom(Class<?> implementation) {
        ClassLoader loader = implementation.getClassLoader();
        String resourceName = implementation.getName().replace('.','/') + ".class";
        InputStream stream = loader.getResourceAsStream(resourceName);
        return Try.of(() -> AgentApiUtils.streamToByteArray(stream)).getOrElseThrow(() -> new RuntimeException(""));
    }


    public Set<String> getInterfaces() {
        return interfaces;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public Option<String> getMixinInit() {
        return mixinInit;
    }
}