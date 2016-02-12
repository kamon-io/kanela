package kamon.agent.api.instrumentation.interceptor;

import net.bytebuddy.asm.AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription.InDefinedShape;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.commons.AdviceAdapter;

public class MethodInterceptorVisitorWrapper implements MethodVisitorWrapper {

    private final InterceptorDescription interceptorDescription;

    public MethodInterceptorVisitorWrapper(InterceptorDescription interceptorDescription) {
        this.interceptorDescription = interceptorDescription;
    }

    @Override
    public MethodVisitor wrap(TypeDescription typeDescription, InDefinedShape shape, MethodVisitor methodVisitor) {
        return new MethodInterceptorVisitor(methodVisitor, shape, interceptorDescription);
    }

    private class MethodInterceptorVisitor extends AdviceAdapter {

        public MethodInterceptorVisitor(MethodVisitor methodVisitor, InDefinedShape shape, InterceptorDescription interceptorDescription) {
            super(Opcodes.ASM5,methodVisitor,shape.getActualModifiers(),shape.getName(), shape.getDescriptor());

        }

        @Override
        protected void onMethodEnter() {
            super.onMethodEnter();
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            super.visitLocalVariable(name, desc, signature, start, end, index);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack, maxLocals);
        }
    }
}