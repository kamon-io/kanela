package kamon.agent.api.instrumentation.mixin;

import net.bytebuddy.jar.asm.*;
import net.bytebuddy.jar.asm.commons.RemappingMethodAdapter;
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

    public MixinClassVisitor(MixinDescription mixin, String className, ClassVisitor classVisitor) {
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
            mn.instructions.resetLabels();
            mn.accept(new RemappingMethodAdapter(mn.access, mn.desc, mv, new SimpleRemapper(cn.name, type.getInternalName())));
        });

        super.visitEnd();
    }

    private static Predicate<MethodNode> isConstructor() {
        return p -> !p.name.equals(ConstructorDescriptor);
    }
}
