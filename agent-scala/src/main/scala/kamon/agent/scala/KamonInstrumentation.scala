/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

package kamon.agent.scala

import java.util.function.{ BiFunction ⇒ JBifunction, Supplier ⇒ JSupplier }

import kamon.agent.api.instrumentation.InstrumentationDescription
import kamon.agent.libs.javaslang.{ Function1 ⇒ JFunction1, Function2 ⇒ JFunction2, Function4 ⇒ JFunction4 }
import kamon.agent.libs.net.bytebuddy.description.`type`.TypeDescription
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.dynamic.DynamicType.Builder
import kamon.agent.libs.net.bytebuddy.implementation.MethodDelegation
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.utility.JavaModule

trait KamonInstrumentation extends kamon.agent.api.instrumentation.KamonInstrumentation {

  implicit def toJavaSupplier[A](f: ⇒ A): JSupplier[A] = new JSupplier[A]() {  def get: A = f }
  implicit def toJavaFunction1[A, B](f: (A) ⇒ B): JFunction1[A, B] = {
    new JFunction1[A, B]() {
      override def apply(t1: A): B = f(t1)
    }
  }

  private implicit def toJavaFunction2[A, B, C](f: (A, B) ⇒ C): JFunction2[A, B, C] = (a: A, b: B) ⇒ f(a, b)
  private implicit def toJavaFunction4[A, B, C, D, E](f: (A, B, C, D) ⇒ E): JFunction4[A, B, C, D, E] = (a: A, b: B, c: C, d: D) ⇒ f(a, b, c, d)

  private implicit def toJavaBiFunction[A, B, C](f: (A, B) ⇒ C): JBifunction[A, B, C] = {
    new JBifunction[A, B, C]() {
      override def apply(t: A, u: B): C = f(t, u)
    }
  }

  def forSubtypeOf(name: String)(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription): Unit = {
    super.forSubtypeOf(name, builder)
  }

  def forTargetType(name: String)(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription): Unit = {
    super.forTargetType(name, builder)
  }

  def forType(typeDescription: => ElementMatcher[_ >: TypeDescription])(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription): Unit = {
    super.forType(typeDescription, builder)
  }

  implicit class PimpInstrumentationBuilder(instrumentationBuilder: InstrumentationDescription.Builder) {
    def withTransformationFor(method: Junction[MethodDescription], delegate: Class[_]) = {
      addTransformation((builder, _, _, _) ⇒ builder.method(method).intercept(MethodDelegation.to(delegate)))
    }
    def addTransformation(f: ⇒ (Builder[_], TypeDescription, ClassLoader, JavaModule) ⇒ Builder[_]) = instrumentationBuilder.withTransformation(f)
  }
}