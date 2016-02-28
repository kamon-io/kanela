package kamon.agent.utils

import java.util.function.{ BiFunction, Supplier }

import kamon.agent.libs.javaslang.Function2

/**
 * Extend the conversions provided by JavaConversions
 */
trait ExtendJavaConversions {
  implicit def toJavaSupplier[A](f: ⇒ A): Supplier[A] = new Supplier[A] {
    override def get(): A = f
  }
  implicit def toJavaFunction2[A, B, C](f: (A, B) ⇒ C): Function2[A, B, C] = new Function2[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  implicit def toJavaBiFunction[A, B, C](f: (A, B) ⇒ C): BiFunction[A, B, C] = new BiFunction[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

}

object ExtendJavaConversions extends ExtendJavaConversions
