/*
 * =========================================================================================
 * Copyright © 2013-2025 the kamon project <http://kamon.io/>
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

package kanela.agent.bytebuddy;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import kanela.agent.api.instrumentation.mixin.Initializer;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.jar.asm.commons.MethodRemapper;
import net.bytebuddy.jar.asm.commons.SimpleRemapper;
import net.bytebuddy.jar.asm.tree.ClassNode;
import net.bytebuddy.jar.asm.tree.MethodNode;
import net.bytebuddy.utility.OpenedClassReader;

/**
 * Merge Two Classes into One, based on [1] and [2]
 *
 * <p>[1]: http://asm.ow2.org/current/asm-transformations.pdf [2]:
 * https://github.com/glowroot/glowroot/blob/master/agent/plugin-api/src/main/java/org/glowroot/agent/plugin/api/weaving/Mixin.java
 */
public class MixinClassVisitor extends ClassVisitor {

  static final String ConstructorDescriptor = "<init>";
  private final Type type;
  private final Class<?> mixinClass;
  private final ClassLoader instrumentationClassLoader;

  public MixinClassVisitor(
      Class<?> mixinClass,
      String className,
      ClassVisitor classVisitor,
      ClassLoader instrumentationClassLoader) {
    super(OpenedClassReader.ASM_API, classVisitor);
    this.mixinClass = mixinClass;
    this.type = Type.getObjectType(className);
    this.instrumentationClassLoader = instrumentationClassLoader;
  }

  private Optional<String> findInitializerMethod() {
    return Arrays.stream(mixinClass.getDeclaredMethods())
        .filter(method -> method.isAnnotationPresent(Initializer.class))
        .findFirst()
        .map(Method::getName);
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String desc, String signature, String[] exceptions) {
    Optional<String> initMethod = findInitializerMethod();
    if (name.equals(ConstructorDescriptor) && initMethod.isPresent()) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
      return new MixinInitializer(mv, access, name, desc, type, initMethod.get());
    }
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  @Override
  public void visitEnd() {
    try {
      String mixinClassFileName = mixinClass.getName().replace('.', '/') + ".class";

      try (InputStream classStream =
          instrumentationClassLoader.getResourceAsStream(mixinClassFileName)) {
        ClassReader cr = new ClassReader(classStream);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        cn.fields.forEach(fieldNode -> fieldNode.accept(this));
        cn.methods.stream()
            .filter(isConstructor())
            .forEach(
                mn -> {
                  String[] exceptions = new String[mn.exceptions.size()];
                  MethodVisitor mv =
                      cv.visitMethod(mn.access, mn.name, mn.desc, mn.signature, exceptions);
                  mn.accept(
                      new MethodRemapper(
                          mv,
                          new SimpleRemapper(
                              OpenedClassReader.ASM_API, cn.name, type.getInternalName())));
                });
      }
      super.visitEnd();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Predicate<MethodNode> isConstructor() {
    return p -> !p.name.equals(ConstructorDescriptor);
  }
}
