package kamon.agent.api.instrumentation.mixin;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;

public class MixinClassVisitorWrapper implements AsmVisitorWrapper {

    private final MixinDescription2 mixin;

    public MixinClassVisitorWrapper(MixinDescription2 mixin) {
        this.mixin = mixin;
    }

    @Override
    public int mergeWriter(int flags) {  return flags; }

    @Override
    public int mergeReader(int flags) { return flags | ClassReader.EXPAND_FRAMES; }

    @Override
    public ClassVisitor wrap(TypeDescription typeDescription, ClassVisitor classVisitor) {
        return new MixinClassVisitor(mixin, classVisitor);
    }
}