package kamon.agent.api.instrumentation.mixin;

import lombok.Value;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.pool.TypePool;

@Value(staticConstructor = "of")
public class MixinClassVisitorWrapper implements AsmVisitorWrapper {

    MixinDescription mixin;

    @Override
    public int mergeWriter(int flags) {  return flags; }

    @Override
    public int mergeReader(int flags) { return flags;}

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, Implementation.Context implementationContext, TypePool typePool, int writerFlags, int readerFlags) {
        return new MixinClassVisitor(mixin, instrumentedType.getInternalName(), classVisitor);
    }
}