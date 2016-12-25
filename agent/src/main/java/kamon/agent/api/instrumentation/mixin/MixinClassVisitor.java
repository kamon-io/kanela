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

import net.bytebuddy.jar.asm.*;
import net.bytebuddy.jar.asm.commons.MethodRemapper;
import net.bytebuddy.jar.asm.commons.SimpleRemapper;
import net.bytebuddy.jar.asm.tree.ClassNode;
import net.bytebuddy.jar.asm.tree.FieldNode;
import net.bytebuddy.jar.asm.tree.MethodNode;

import java.util.*;
import java.util.function.Predicate;

public class MixinClassVisitor extends ClassVisitor {

    private static final String ConstructorDescriptor = "<init>";

    private final MixinDescription mixin;
    private final Type type;

    MixinClassVisitor(MixinDescription mixin, String className, ClassVisitor classVisitor) {
        super(Opcodes.ASM5, classVisitor);
        this.mixin = mixin;
        this.type = Type.getObjectType(className);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals(ConstructorDescriptor) && mixin.getMixinInit().isDefined()) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new MixinInitializer(mv, access, name, desc, type, mixin);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, addInterfaces(mixin, interfaces));
    }

    private String[] addInterfaces(MixinDescription mixin, String[] interfaces) {
        if(mixin.getInterfaces().isEmpty()) return interfaces;
        Set<String> newInterfaces = mixin.getInterfaces();
        if (interfaces != null) newInterfaces.addAll(Arrays.asList(interfaces));
        return newInterfaces.toArray(new String[newInterfaces.size()]);
    }


    @Override
    public void visitEnd() {
        ClassReader cr = new ClassReader(mixin.getBytes());
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        ((List<FieldNode>) cn.fields).forEach(fieldNode -> fieldNode.accept(this));
        ((List<MethodNode>) cn.methods).stream().filter(isConstructor()).forEach(mn -> {
            String[] exceptions = new String[mn.exceptions.size()];
            MethodVisitor mv = cv.visitMethod(mn.access, mn.name, mn.desc, mn.signature, exceptions);
            mn.accept(new MethodRemapper(mv, new SimpleRemapper(cn.name, type.getInternalName())));
        });

        super.visitEnd();
    }

    private static Predicate<MethodNode> isConstructor() {
        return p -> !p.name.equals(ConstructorDescriptor);
    }
}
