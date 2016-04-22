package app.kamon.instrumentation

import java.util.concurrent.Callable

import kamon.agent.api.instrumentation.Initializer
import kamon.agent.libs.net.bytebuddy.asm.Advice.{Enter, OnMethodEnter, OnMethodExit}
import kamon.agent.libs.net.bytebuddy.implementation.bind.annotation.{RuntimeType, SuperCall}
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers.named
import kamon.agent.scala

class CustomInstrumentation extends scala.KamonInstrumentation {

  forTargetType("app.kamon.instrumentation.Pepe") { builder =>
    builder.withMixin(classOf[MixinTest])
           .withAdvisorFor(named("hello"), classOf[MethodAdvisor])
           .build()
  }

  forSubtypeOf("app.kamon.instrumentation.Pepe") { builder =>
    builder.withAdvisorFor(named("bye"), classOf[MethodAdvisor])
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
    def onMethodEnter():Long = {
      System.currentTimeMillis()
//      println("Esto es MUY GROSOOOOOOOOO --- ENTER")
    }

    @OnMethodExit
    def onMethodExit(@Enter start:Long):Unit = {

      println("method took " + (System.currentTimeMillis() - start))
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

final case class Pepe() {
  def hello() = println("Hi, all")
  def bye() = {println("good bye");Thread.sleep(100)}
}