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

package kanela.agent.api.instrumentation.mixin;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import net.bytebuddy.jar.asm.*;
import net.bytebuddy.jar.asm.commons.MethodRemapper;
import net.bytebuddy.jar.asm.commons.SimpleRemapper;
import net.bytebuddy.jar.asm.tree.ClassNode;
import net.bytebuddy.jar.asm.tree.MethodNode;

import java.util.function.Predicate;

/**
 * Merge Two Classes into One, based on [1] and [2]
 *
 * [1]: http://asm.ow2.org/current/asm-transformations.pdf
 * [2]: https://github.com/glowroot/glowroot/blob/master/agent/plugin-api/src/main/java/org/glowroot/agent/plugin/api/weaving/Mixin.java
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class MixinClassVisitor extends ClassVisitor {

    static final String ConstructorDescriptor = "<init>";

    MixinDescription mixin;
    Type type;

    public static MixinClassVisitor from(MixinDescription mixin, String className, ClassVisitor classVisitor) {
        return new MixinClassVisitor(mixin, className, classVisitor);
    }

    private MixinClassVisitor(MixinDescription mixin, String className, ClassVisitor classVisitor) {
        super(Opcodes.ASM6, classVisitor);
        this.mixin = mixin;
        this.type = Type.getObjectType(className);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals(ConstructorDescriptor) && mixin.getInitializerMethod().isDefined()) {
            val mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new MixinInitializer(mv, access, name, desc, type, mixin);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void visitEnd() {
        // By default, ClassReader will try to use the System ClassLoader to load the classes but we need to make sure
        // that all classes are loaded with Kanela's ClassLoader (which some times might be the System ClassLoader and
        // some others will be an Attach ClassLoader).
        val classLoader = Thread.currentThread().getContextClassLoader();
        val mixinClassFileName = mixin.getMixinClass().getName().replace('.', '/') + ".class";
        val classStream = classLoader.getResourceAsStream(mixinClassFileName);

        val cr = new ClassReader(classStream);
        classStream.close();

        val cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        cn.fields.forEach(fieldNode -> fieldNode.accept(this));
        cn.methods.stream().filter(isConstructor()).forEach(mn -> {
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
