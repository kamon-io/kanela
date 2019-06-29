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

package kanela.agent.api.instrumentation.bridge;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.jar.asm.commons.Method;
import net.bytebuddy.utility.OpenedClassReader;

import java.lang.annotation.Annotation;


@Value
@EqualsAndHashCode(callSuper = false)
public class BridgeClassVisitor extends ClassVisitor {

    BridgeDescription bridge;
    Type type;

    public static BridgeClassVisitor from(BridgeDescription bridge, String className, ClassVisitor classVisitor) {
        return new BridgeClassVisitor(bridge, className, classVisitor);
    }

    private BridgeClassVisitor(BridgeDescription bridge, String className, ClassVisitor classVisitor) {
        super(OpenedClassReader.ASM_API, classVisitor);
        this.bridge = bridge;
        this.type = Type.getObjectType(className);
    }

    @Override
    public void visitEnd() {
       bridge.getMethods().forEach(reflectMethod -> {
           for(Annotation annotation : reflectMethod.getDeclaredAnnotations()) {
               if(annotation instanceof Bridge) processBridge(reflectMethod, annotation);
               else if (annotation instanceof FieldBridge) processFieldBridge(reflectMethod, annotation);
           }
       });
       cv.visitEnd();
    }

    private void processBridge(java.lang.reflect.Method reflectMethod, Annotation annotation) {
        val bridge = (Bridge) annotation;
        val method = Method.getMethod(reflectMethod);
        val targetMethod = Method.getMethod(bridge.value());

        val mv = cv.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), method.getDescriptor(), null, null);
        mv.visitCode();
        int i = 0;
        mv.visitVarInsn(Opcodes.ALOAD, i++);

        for (Type argument : method.getArgumentTypes()) {
            mv.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), i++);
        }

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, type.getInternalName(), targetMethod.getName(), targetMethod.getDescriptor(), false);
        mv.visitInsn(method.getReturnType().getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void processFieldBridge(java.lang.reflect.Method reflectMethod, Annotation annotation) {
        val fieldBridge = (FieldBridge) annotation;
        val method = Method.getMethod(reflectMethod);

        val mv = cv.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), method.getDescriptor(), null, null);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, type.getInternalName(), fieldBridge.value(), method.getReturnType().getDescriptor());
        mv.visitInsn(Type.getType(type.getDescriptor()).getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
