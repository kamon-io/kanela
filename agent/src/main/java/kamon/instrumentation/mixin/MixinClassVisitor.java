package kamon.instrumentation.mixin;

import javaslang.collection.List;
import net.bytebuddy.jar.asm.*;
import net.bytebuddy.jar.asm.commons.RemappingMethodAdapter;
import net.bytebuddy.jar.asm.commons.SimpleRemapper;
import net.bytebuddy.jar.asm.tree.ClassNode;
import net.bytebuddy.jar.asm.tree.FieldNode;
import net.bytebuddy.jar.asm.tree.MethodNode;

import java.util.function.Predicate;

public class MixinClassVisitor extends ClassVisitor {

    private Type className;
    private static final String ConstructorDescriptor = "<init>";
    private MixinDescription mixin;

    public MixinClassVisitor(MixinDescription mixin, ClassVisitor classVisitor) {
        super(Opcodes.ASM5, classVisitor);
        this.mixin = mixin;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals(ConstructorDescriptor) && mixin.getMixinInit().isDefined()) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new MixinInitializer(mv, access, name, desc, this.className, mixin);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = Type.getObjectType(name);
        List<String> newInterfaces = List.of(interfaces);

        if(!mixin.getInterfaces().isEmpty()){
            newInterfaces.appendAll(mixin.getInterfaces());
        }
        cv.visit(version, access, name, signature, superName, newInterfaces.toJavaArray(String.class));
    }

    @Override
    public void visitEnd() {
        ClassReader cr = new ClassReader(mixin.getBytes());
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        ((List<FieldNode>) cn.fields).forEach(fieldNode -> fieldNode.accept(this));
        ((List<MethodNode>) cn.methods).filter(isConstructor()).forEach(mn -> {
            String[] exceptions = new String[mn.exceptions.size()];
            MethodVisitor mv = cv.visitMethod(mn.access, mn.name, mn.desc, mn.signature, exceptions);
            mn.instructions.resetLabels();
            mn.accept(new RemappingMethodAdapter(mn.access, mn.desc, mv, new SimpleRemapper(cn.name, className.getInternalName())));
        });

        super.visitEnd();
    }

    private static Predicate<MethodNode> isConstructor() {
        return p -> !p.name.equals(ConstructorDescriptor);
    }
}
