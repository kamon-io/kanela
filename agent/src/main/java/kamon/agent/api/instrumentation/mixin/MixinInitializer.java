/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

package kamon.agent.api.instrumentation.mixin;

import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.jar.asm.commons.AdviceAdapter;
import net.bytebuddy.jar.asm.commons.Method;

public class MixinInitializer extends AdviceAdapter {
    private static final String ConstructorDescriptor = "<init>";

    private final Type typeClass;
    private final MixinDescription mixinDescription;
    private boolean cascadingConstructor;


    MixinInitializer(MethodVisitor mv, int access, String name, String desc, Type typeClass, MixinDescription mixinDescription) {
        super(Opcodes.ASM5, mv, access, name, desc);
        this.typeClass = typeClass;
        this.mixinDescription = mixinDescription;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (name.equals(ConstructorDescriptor) && owner.equals(typeClass.getInternalName())) cascadingConstructor = true;
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (cascadingConstructor) return;
        mixinDescription.getMixinInit().forEach(methodName -> {
            loadThis();
            invokeVirtual(typeClass, new Method(methodName, "()V"));
        });
    }
}