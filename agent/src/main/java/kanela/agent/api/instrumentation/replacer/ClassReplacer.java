/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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


package kanela.agent.api.instrumentation.replacer;

import io.vavr.control.Try;
import kanela.agent.util.annotation.Experimental;
import kanela.agent.util.conf.KanelaConfiguration.ClassReplacerConfig;
import kanela.agent.util.log.Logger;
import lombok.Value;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.commons.ClassRemapper;
import net.bytebuddy.jar.asm.commons.SimpleRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static java.text.MessageFormat.format;

@Value
@Experimental
public class ClassReplacer {

    ClassReplacerConfig classReplacerConfig;

    public static void attach(Instrumentation instrumentation, ClassReplacerConfig configuration) {
        if(configuration.getClassesToReplace().nonEmpty()) {
            Try.run(() -> instrumentation.addTransformer(new ClassReplacerTransformer(configuration.getClassesToReplace())))
                    .andThen(() -> Logger.info(() -> format("Class Replacer activated.")))
                    .onFailure((cause) -> Logger.error(() -> "Error when trying to activate Class Replacer.", cause));

        }
    }

    @Value
    static class ClassReplacerTransformer implements ClassFileTransformer {
        io.vavr.collection.Map<String, String> classesToReplace;

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            return classesToReplace.get(className).map(resource -> {
                System.out.println("contains " + className);
                return Try.of(() -> getBytesFromResource(className, resource, loader))
                        .onFailure(cause -> Logger.error(() -> "Error trying to Replace Class: " + className, cause))
                        .getOrElse(() -> classfileBuffer);
            }).getOrElse(classfileBuffer);
        }

        private static byte[] getBytesFromResource(String classToReplace, String resource, ClassLoader loader) throws IOException {
            try(InputStream in = loader.getResourceAsStream(resource + ".class")) {
                ClassReader reader = new ClassReader(in);
                ClassWriter classWriter = new ClassWriter(0);
                reader.accept(new ClassRemapper(classWriter, new SimpleRemapper(resource, classToReplace)), ClassReader.EXPAND_FRAMES);
                return classWriter.toByteArray();
            }
        }
    }
}
