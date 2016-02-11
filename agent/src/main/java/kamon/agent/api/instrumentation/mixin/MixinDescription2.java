package kamon.agent.api.instrumentation.mixin;

import javaslang.control.Option;
import javaslang.control.Try;
import kamon.agent.api.instrumentation.initializer;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MixinDescription2 {

    private final Type implementation;
    private final List<String> interfaces;
    private final byte[] bytes;
    private final Option<String> mixinInit;
    private final ElementMatcher targetTypes;

    public MixinDescription2(Type implementation,
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

    public static MixinDescription2 of(ElementMatcher targetTypes, Class<?> clazz) {
        Type implementation = Type.getType(clazz);
        List<String> interfaces = Arrays.stream(clazz.getInterfaces()).map(name -> Type.getType(name).getInternalName()).collect(Collectors.toList());
        Option<String> mixinInit = Option.of(Arrays.stream(clazz.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(initializer.class)).findFirst().get().getName());
        return new MixinDescription2(implementation, interfaces, getBytesFrom(clazz), mixinInit, targetTypes);
    }

    private static byte[] getBytesFrom(Class<?> implementation) {
        ClassLoader loader = implementation.getClassLoader();
        String resourceName = implementation.getName().replace('.','/') + ".class";
        InputStream stream = loader.getResourceAsStream(resourceName);
        return Try.of(() -> streamToByteArray(stream)).getOrElseThrow(() -> new RuntimeException(""));
    }


    public List<String> getInterfaces() {
        return interfaces;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public Option<String> getMixinInit() {
        return mixinInit;
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

        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int line = 0;
        // read bytes from stream, and store them in buffer
        while ((line = stream.read(buffer)) != -1) {
            // Writes bytes from byte array (buffer) into output stream.
            os.write(buffer, 0, line);
        }
        stream.close();
        os.flush();
        os.close();
        return os.toByteArray();
    }
}