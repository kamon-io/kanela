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

package kanela.agent.api.instrumentation.legacy;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.utility.OpenedClassReader;

import static net.bytebuddy.ClassFileVersion.JAVA_V5;

@Value
@EqualsAndHashCode(callSuper = false)
public class ClassFileVersionValidatorClassVisitor extends ClassVisitor {

    public static ClassFileVersionValidatorClassVisitor from(ClassVisitor classVisitor) {
        return new ClassFileVersionValidatorClassVisitor(classVisitor);
    }

    private ClassFileVersionValidatorClassVisitor(ClassVisitor classVisitor) {
        super(OpenedClassReader.ASM_API, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        val classFileVersion = ClassFileVersion.ofMinorMajor(version);

        if (classFileVersion.isLessThan(JAVA_V5))
            throw NoStackTraceUnsupportedClassFileVersion.Instance;

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class NoStackTraceUnsupportedClassFileVersion extends RuntimeException {

        public static NoStackTraceUnsupportedClassFileVersion Instance = new NoStackTraceUnsupportedClassFileVersion();

        private NoStackTraceUnsupportedClassFileVersion() {}

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
}
