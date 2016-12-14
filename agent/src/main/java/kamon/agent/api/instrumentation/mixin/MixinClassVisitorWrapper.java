package kamon.agent.api.instrumentation.mixin;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.pool.TypePool;

public class MixinClassVisitorWrapper implements AsmVisitorWrapper {

    private final MixinDescription mixin;

    public MixinClassVisitorWrapper(MixinDescription mixin) {
        this.mixin = mixin;
    }

    @Override
    public int mergeWriter(int flags) {  return flags; }

    @Override
    public int mergeReader(int flags) { return flags;}

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, Implementation.Context implementationContext, TypePool typePool, int writerFlags, int readerFlags) {
        return new MixinClassVisitor(mixin, instrumentedType.getInternalName(), classVisitor);
    }
}