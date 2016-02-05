/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.api.instrumentation.mixin

import kamon.api.instrumentation.initializer
import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.jar.asm._
import net.bytebuddy.jar.asm.commons.{ AdviceAdapter, Method, RemappingMethodAdapter, SimpleRemapper }
import net.bytebuddy.jar.asm.tree._
import net.bytebuddy.matcher.ElementMatcher
import scala.language.existentials

case class MixinDescription(implementation: Type,
  interfaces: Array[String],
  bytes: Array[Byte],
  mixinInit: Option[String],
  targetTypes: ElementMatcher[_ >: TypeDescription])

object MixinDescription {

  def apply(targetTypes: ElementMatcher[_ >: TypeDescription], clazz: Class[_]): MixinDescription = {
    val implementation = Type.getType(clazz)
    val interfaces: Array[String] = clazz.getInterfaces.map(Type.getType(_).getInternalName)
    val mixinInit = clazz.getDeclaredMethods.find(_.isAnnotationPresent(classOf[initializer])).map(_.getName)
    new MixinDescription(implementation, interfaces, getBytesFrom(clazz), mixinInit, targetTypes)
  }

  private def getBytesFrom(implementation: Class[_]) = {
    val loader = implementation.getClassLoader
    val resourceName = s"${implementation.getName.replace('.', '/')}.class"
    val stream = loader.getResourceAsStream(resourceName)
    Stream.continually(stream.read).takeWhile(_ != -1).map(_.toByte).toArray
  }
}

class MixinClassVisitor(mixin: MixinDescription, classVisitor: ClassVisitor) extends ClassVisitor(Opcodes.ASM5, classVisitor) {
  import scala.collection.JavaConversions._

  val ConstructorDescriptor: String = "<init>"
  var className: Type = _

  override def visit(version: Int, access: Int, name: String, signature: String, superName: String, interfaces: Array[String]): Unit = {
    this.className = Type.getObjectType(name)
    val newInterfaces = if (Option(interfaces).isEmpty) mixin.interfaces else interfaces ++ mixin.interfaces
    cv.visit(version, access, name, signature, superName, newInterfaces)
  }

  override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]): MethodVisitor = {
    if (name.equals(ConstructorDescriptor) && mixin.mixinInit.isDefined) {
      val mv = super.visitMethod(access, name, desc, signature, exceptions)
      return MixinInitializer(mv, access, name, desc, this.className, mixin)
    }
    super.visitMethod(access, name, desc, signature, exceptions)
  }

  override def visitEnd(): Unit = {
    val constructor: (Any) ⇒ Boolean = method ⇒ method.asInstanceOf[MethodNode].name.equals(ConstructorDescriptor)
    val cr = new ClassReader(mixin.bytes)
    val cn = new ClassNode
    cr.accept(cn, ClassReader.EXPAND_FRAMES)

    cn.fields.foreach(_.asInstanceOf[FieldNode].accept(this))
    cn.methods.filterNot(constructor).foreach {
      method ⇒
        val mn: MethodNode = method.asInstanceOf[MethodNode]
        val exceptions = new Array[String](mn.exceptions.size())
        val mv = cv.visitMethod(mn.access, mn.name, mn.desc, mn.signature, exceptions)

        mn.instructions.resetLabels()
        mn.accept(new RemappingMethodAdapter(mn.access, mn.desc, mv, new SimpleRemapper(cn.name, className.getInternalName)))
    }
    super.visitEnd()
  }

  class MixinInitializer private (mv: MethodVisitor, access: Int, name: String, desc: String, typeClass: Type, mixinDescription: MixinDescription) extends AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {
    var cascadingConstructor: Boolean = _

    override def visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean): Unit = {
      if (name.equals(ConstructorDescriptor) && owner.equals(typeClass.getInternalName)) cascadingConstructor = true
      super.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    override def onMethodExit(opcode: Int): Unit = {
      if (!cascadingConstructor) {
        mixinDescription.mixinInit.foreach {
          methodName ⇒
            loadThis()
            invokeVirtual(typeClass, new Method(methodName, "()V"))
        }
      }
    }
  }

  object MixinInitializer {
    def apply(mv: MethodVisitor, access: Int, name: String, desc: String, typeClass: Type, mixinDescription: MixinDescription): MixinInitializer = {
      new MixinInitializer(mv, access, name, desc, typeClass, mixinDescription)
    }
  }
}