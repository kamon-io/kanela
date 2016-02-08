package kamon.agent.api.instrumentation.mixin;

import javaslang.control.Option;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MixinDescription {

    private final Type implementation;
    private final List<String> interfaces;
    private final byte[] bytes;
    private final Option<String> mixinInit;
    private final ElementMatcher targetTypes;

    public MixinDescription(Type implementation,
                            List<String> interfaces,
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
        return null;
    }

    private static byte[] getBytesFrom(Class<?> implementation) {
        return null;
    }


    public List<String> getInterfaces() {
        return null;
    }

    public byte[] getBytes() {
        return null;
    }

    public Option<String> getMixinInit() {
        return null;
    }

    /**
     * TODO:move to Utils or something like that
     *
     * method converts {@link InputStream} Object into byte[] array.
     *
     * @param stream the {@link InputStream} Object.
     * @return the byte[] array representation of received {@link InputStream} Object.
     * @throws IOException if an error occurs.
     */
    private static byte[] streamToByteArray(InputStream stream) throws IOException {
        return null;
    }
}