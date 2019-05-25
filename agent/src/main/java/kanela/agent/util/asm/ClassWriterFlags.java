package kanela.agent.util.asm;

import io.vavr.control.Try;
import lombok.Value;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader;
import net.bytebuddy.jar.asm.ClassWriter;

@Value
public class ClassWriterFlags {
    /**
     *
     * @param typeDescription
     * @param classLoader
     * @return
     */
    public static int from(TypeDescription typeDescription, ClassLoader classLoader) {
        return Try.of(() -> {
            if (ClassFileVersion.of(typeDescription, ForClassLoader.of(classLoader)).isGreaterThan(ClassFileVersion.JAVA_V5))
                return ClassWriter.COMPUTE_FRAMES;
            return ClassWriter.COMPUTE_MAXS;
        }).getOrElse(ClassWriter.COMPUTE_FRAMES);
    }
}
