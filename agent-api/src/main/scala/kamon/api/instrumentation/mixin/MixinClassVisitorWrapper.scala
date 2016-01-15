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

import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.jar.asm.{ ClassReader, ClassVisitor }
case class MixinClassVisitorWrapper(mixin: MixinDescription) extends AsmVisitorWrapper {
  override def mergeWriter(flags: Int): Int = flags
  override def mergeReader(flags: Int): Int = flags | ClassReader.EXPAND_FRAMES
  override def wrap(typeDescription: TypeDescription, classVisitor: ClassVisitor): ClassVisitor = new MixinClassVisitor(mixin, classVisitor)
}
