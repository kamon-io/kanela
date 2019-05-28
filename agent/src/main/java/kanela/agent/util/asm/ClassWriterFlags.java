/*
 * =========================================================================================
 * Copyright © 2013-2019 the kamon project <http://kamon.io/>
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


package kanela.agent.util.asm;

import io.vavr.control.Try;
import kanela.agent.util.log.Logger;
import lombok.Value;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader;
import net.bytebuddy.jar.asm.ClassWriter;

@Value
public class ClassWriterFlags {

    /**
     * In versions of the JVM  &gt; 1.5 the classes bytecode contain a stack map along with the method code. This map describes the layout of the stack at key points (jump targets) during the method's execution.
     *
     * In previous versions(JVM &lt; 1.5 bytecode), the JVM would have to compute this information, which is computationally expensive.
     *
     * By requiring this information, the JVM can just verify that the frames work, which is significantly easier than recalculating everything.
     *
     * So if JVM &lt; 1.5 ClassWriter.COMPUTE_MAXS otherwise ClassWriter.COMPUTE_FRAME(computeFrames implies computeMaxs).
     */
    public static int resolve(TypeDescription typeDescription, ClassLoader classLoader) {
        if (classFileVersionIsGreaterThan(ClassFileVersion.JAVA_V5, typeDescription, classLoader)) return ClassWriter.COMPUTE_FRAMES;
        return ClassWriter.COMPUTE_MAXS;
    }

    private static boolean classFileVersionIsGreaterThan(ClassFileVersion classFileVersion, TypeDescription typeDescription, ClassLoader classLoader) {
        return Try.of(() -> ClassFileVersion.of(typeDescription, ForClassLoader.of(classLoader)).isGreaterThan(classFileVersion))
                .onFailure(error -> Logger.error(error::getMessage, error))
                .getOrElse(true);
    }
}
