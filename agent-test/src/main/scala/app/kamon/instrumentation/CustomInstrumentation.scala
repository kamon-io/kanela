package app.kamon.instrumentation

import java.util.concurrent.Callable
import java.util.function.{ BiFunction ⇒ JBifunction, Supplier ⇒ JSupplier }
import kamon.agent.libs.net.bytebuddy.implementation.bind.annotation.{ SuperCall, RuntimeType }
import kamon.agent.libs.javaslang.{ Function2 ⇒ JFunction2 }
import kamon.agent.api.instrumentation.{ after, before, initializer, KamonInstrumentation }
import kamon.agent.libs.net.bytebuddy.description.`type`.TypeDescription
import kamon.agent.libs.net.bytebuddy.dynamic.DynamicType
import kamon.agent.libs.net.bytebuddy.implementation.MethodDelegation
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers

class CustomInstrumentation extends KamonInstrumentation {

  implicit def toJavaSupplier[A](f: ⇒ A): JSupplier[A] = new JSupplier[A] {
    override def get(): A = f
  }

  implicit def toJavaFunction2[A, B, C](f: (A, B) ⇒ C): JFunction2[A, B, C] = new JFunction2[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  implicit def toJavaBiFunction[A, B, C](f: (A, B) ⇒ C): JBifunction[A, B, C] = new JBifunction[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  forTargetType("app.kamon.instrumentation.Pepe")

  addMixin(classOf[MixinTest])

  addTransformation { (builder: DynamicType.Builder[_], _: TypeDescription) ⇒
    builder
      .method(ElementMatchers.named("hello"))
      .intercept(MethodDelegation.to(PepeInterceptor).filter(NotDeclaredByObject))
  }

  class MixinTest extends Serializable {
    var a: String = _

    @initializer
    def init() = this.a = { println("HeeeeeLooooo"); "papa" }
  }

  object PepeInterceptor {
    @RuntimeType
    def prepareStatement(@SuperCall callable: Callable[_]): Any = {
      println("puto")
      val prepareStatement = callable.call()
      prepareStatement
    }
  }
}

class Pepe() {
  def hello() = println("asdfasklñfjaskfjasdkl")
}