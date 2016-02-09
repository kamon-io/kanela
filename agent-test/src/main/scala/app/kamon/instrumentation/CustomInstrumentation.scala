package app.kamon.instrumentation

import java.util.function.Supplier

import kamon.agent.api.instrumentation.KamonInstrumentation

class CustomInstrumentation extends KamonInstrumentation {

  implicit def toJavaSupplier[A](f: ⇒ A): Supplier[A] = new Supplier[A] {
    override def get(): A = f
  }

  forTargetType("TargetTest")
}
