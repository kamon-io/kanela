package app.kamon.instrumentation

import java.util.concurrent.Callable

import kamon.agent.api.instrumentation.initializer
import kamon.agent.libs.net.bytebuddy.implementation.MethodDelegation
import kamon.agent.libs.net.bytebuddy.implementation.bind.annotation.{RuntimeType, SuperCall}
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers
import kamon.agent.scala

class CustomInstrumentation extends scala.KamonInstrumentation {

  forTargetType("app.kamon.instrumentation.Pepe")

  addMixin(classOf[MixinTest])

  addTransformation { (builder, _) ⇒
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
      callable.call()
    }
  }
}

class Pepe() {
  def hello() = println("Hi, all")
}