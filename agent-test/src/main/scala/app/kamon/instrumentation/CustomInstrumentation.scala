package app.kamon.instrumentation

import java.util.concurrent.Callable

import kamon.agent.api.instrumentation.{InstrumentationDescription, Initializer}
import kamon.agent.libs.net.bytebuddy.asm.Advice.{OnMethodEnter, OnMethodExit}
import kamon.agent.libs.net.bytebuddy.implementation.bind.annotation.{RuntimeType, SuperCall}
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers.named
import kamon.agent.scala

class CustomInstrumentation extends scala.KamonInstrumentation {

  forSubtypeOf("app.kamon.instrumentation.Pepe") { builder =>
    builder.withMixin(classOf[MixinTest])
           .wihtAdvisor(named("hello"), classOf[MethodAdvisor])
           .build()
  }


  forSubtypeOf("app.kamon.instrumentation.Pepe") { builder =>
    builder.wihtAdvisor(named("bye"), classOf[MethodAdvisor])
      .build()
  }


//  addMixin(classOf[MixinTest])

//  addAdvisor(named("hello"), classOf[MethodAdvisor])

  //  addTransformation { (builder, _, _) ⇒
  //    builder
  //      .method(ElementMatchers.named("hello"))
  //      .intercept(MethodDelegation.to(PepeInterceptor).filter(NotDeclaredByObject))
  //  }
}

  class MethodAdvisor
  object MethodAdvisor {
    @OnMethodEnter
    def onMethodEnter():Unit = {
      println("Esto es MUY GROSOOOOOOOOO --- ENTER")
    }

    @OnMethodExit
    def onMethodExit():Unit = {
      println("Esto es MUY GROSOOOOOOOOO --- EXIT")
    }
  }

  class MixinTest extends Serializable {
    var a: String = _

    @Initializer
    def init() = this.a = { println("HeeeeeLooooo"); "papa" }

  }

  object PepeInterceptor {
    @RuntimeType
    def prepareStatement(@SuperCall callable: Callable[_]): Any = {
      callable.call()
    }
}

class Pepe() {
  def hello() = println("Hi, all")
  def bye() = println("good bye")
}