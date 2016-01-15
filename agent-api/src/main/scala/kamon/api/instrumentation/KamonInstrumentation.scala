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

package kamon.api.instrumentation

import java.lang.instrument.Instrumentation

import kamon.api.instrumentation.listener.InstrumentationListener
import kamon.api.instrumentation.mixin.{ MixinClassVisitorWrapper, MixinDescription }
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.agent.builder.AgentBuilder.Transformer
import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.DynamicType.Builder
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatcher.Junction
import net.bytebuddy.matcher.ElementMatchers._
import net.bytebuddy.pool.TypePool

import scala.collection.mutable

abstract class KamonInstrumentation {
  private var elementMatcher: ElementMatcher[_ >: TypeDescription] = _
  private val mixins = mutable.ListBuffer[MixinDescription]()
  private val transformers = mutable.ListBuffer[Transformer]()

  val typePool = TypePool.Default.ofClassPath()
  val NotDeclaredByObject: Junction[MethodDescription] = not(isDeclaredBy(classOf[Object]))
  val NotTakesArguments: Junction[MethodDescription] = not(takesArguments(0))

  //TODO:configure listener
  def register(instrumentation: Instrumentation): Unit = {
    val builder = new AgentBuilder.Default()
      .withListener(InstrumentationListener())
      .`type`(elementMatcher)

    mixins.foreach { mixin ⇒
      builder
        .transform(withTransformer((builder, _) ⇒ builder.visit(MixinClassVisitorWrapper(mixin))))
        .installOn(instrumentation)
    }
    transformers.foreach(transformer ⇒ builder.transform(transformer).installOn(instrumentation))
  }

  def withTransformer(f: ⇒ (Builder[_], TypeDescription) ⇒ Builder[_]) = new Transformer {
    override def transform(builder: Builder[_], typeDescription: TypeDescription): Builder[_] = {
      f.apply(builder, typeDescription)
    }
  }

  def addTransformation(f: ⇒ (Builder[_], TypeDescription) ⇒ Builder[_]): Unit = transformers += withTransformer(f)
  def forTypes(f: ⇒ ElementMatcher[_ >: TypeDescription]): Unit = elementMatcher = f
  def forType(f: ⇒ ElementMatcher[_ >: TypeDescription]): Unit = forTypes(f)
  def forTargetType(f: ⇒ String): Unit = forType(named(f))
  def forSubtypeOf(f: ⇒ String): Unit = forType(isSubTypeOf(typePool.describe(f).resolve()).and(not(isInterface())))
  def addMixin(clazz: ⇒ Class[_]): Unit = mixins += MixinDescription(elementMatcher, clazz)
}