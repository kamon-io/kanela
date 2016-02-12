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
    private Boolean cascadingConstructor;


    protected MixinInitializer(MethodVisitor mv, int access, String name, String desc, Type typeClass, MixinDescription mixinDescription) {
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
        mixinDescription.getMixinInit().forEach(methodName ->{
            loadThis();
            invokeVirtual(typeClass, new Method(methodName, "()V"));
        });
    }
}