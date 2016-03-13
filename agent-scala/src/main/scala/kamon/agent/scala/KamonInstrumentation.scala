package kamon.agent.scala

import java.util.function.{ BiFunction ⇒ JBifunction, Supplier ⇒ JSupplier }

import kamon.agent.libs.javaslang.{ Function2 ⇒ JFunction2 }
import kamon.agent.libs.javaslang.{ Function3 ⇒ JFunction3 }
import kamon.agent.libs.net.bytebuddy.agent.builder.AgentBuilder.Transformer
import kamon.agent.libs.net.bytebuddy.description.`type`.TypeDescription
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.dynamic.DynamicType.Builder
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction

trait KamonInstrumentation extends kamon.agent.api.instrumentation.KamonInstrumentation {

  implicit def toJavaSupplier[A](f: ⇒ A): JSupplier[A] = new JSupplier[A] {
    override def get(): A = f
  }

  private implicit def toJavaFunction2[A, B, C](f: (A, B) ⇒ C): JFunction2[A, B, C] = new JFunction2[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  private implicit def toJavaFunction3[A, B, C, D](f: (A, B, C) ⇒ D): JFunction3[A, B, C, D] = new JFunction3[A, B, C, D] {
    override def apply(a: A, b: B, c: C): D = f(a, b, c)
  }

  private implicit def toJavaBiFunction[A, B, C](f: (A, B) ⇒ C): JBifunction[A, B, C] = new JBifunction[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  def withTransformer(f: ⇒ (Builder[_], TypeDescription, ClassLoader) ⇒ Builder[_]) = new Transformer {
    override def transform(builder: Builder[_], typeDescription: TypeDescription, classLoader: ClassLoader): Builder[_] = {
      f.apply(builder, typeDescription, classLoader)
    }
  }

  def addTransformation(f: ⇒ (Builder[_], TypeDescription, ClassLoader) ⇒ Builder[_]): Unit = {
    super.addTransformation(f)
  }
}