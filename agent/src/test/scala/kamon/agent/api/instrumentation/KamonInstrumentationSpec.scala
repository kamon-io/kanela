package kamon.agent.api.instrumentation

import javaslang.{Function2 => JFunction2}

import kamon.api.instrumentation.KamonInstrumentation

import java.util.function.{ Supplier ⇒ JSupplier, BiFunction ⇒ JBifunction}

import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.description.`type`.TypeDescription.Generic.Builder
import net.bytebuddy.dynamic.DynamicType


class KamonInstrumentationSpec extends KamonInstrumentation {

  implicit def toJavaSupplier[A](f: =>A) = new JSupplier[A] {
    override def get(): A = f
  }

  implicit def toJavaFunction2[A,B,C](f: (A,B)=> C) = new JFunction2[A,B,C] {
    override def apply(a: A, b: B): C = f(a,b)
  }

  implicit def toJavaBiFunction[A,B,C](f: (A,B)=> C) = new JBifunction[A,B,C] {
    override def apply(a: A, b: B): C = f(a,b)
  }

  forSubtypeOf("java.sql.Connection")



  var a = (builder:DynamicType.Builder[_],t:TypeDescription)=>
    builder
    .method(named ("prepareStatement").and (NotTakesArguments) )
    .intercept (to (ConnectionInterceptor).filter (NotDeclaredByObject) )


  addTransformation(builder:DynamicType.Builder[_],t:TypeDescription)=>
    builder
      .method(named("prepareStatement").and(NotTakesArguments))
      .intercept(to(ConnectionInterceptor).filter(NotDeclaredByObject))
  }
}
