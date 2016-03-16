package kamon.agent.api.instrumentation

import java.lang.instrument.Instrumentation
import java.sql.SQLException
import java.util.concurrent.Callable
import java.util.function.{ BiFunction ⇒ JBifunction, Supplier ⇒ JSupplier }
import javaslang.{ Function2 ⇒ JFunction2 }
import javaslang.{ Function1 ⇒ JFunction1 }

import net.bytebuddy.implementation.bind.annotation.{ Argument, RuntimeType, SuperCall }
import net.bytebuddy.matcher.ElementMatchers._
import org.mockito.Mockito._
import org.scalatest.{ Matchers, WordSpecLike }

class KamonInstrumentationSpec extends WordSpecLike with Matchers {

  implicit def toJavaSupplier[A](f: ⇒ A): JSupplier[A] = new JSupplier[A] {
    override def get(): A = f
  }

  implicit def toJavaFunction2[A, B, C](f: (A, B) ⇒ C): JFunction2[A, B, C] = new JFunction2[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  implicit def toJavaFunction1[A, B](f: (A) ⇒ B): JFunction1[A, B] = new JFunction1[A, B] {
    override def apply(a: A): B = f(a)
  }


  implicit def toJavaBiFunction[A, B, C](f: (A, B) ⇒ C): JBifunction[A, B, C] = new JBifunction[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  "should validate that there is an element selected by elementMatcher" in {
    val kamonInstrumentation = new KamonInstrumentation() {}
    val fakeInstrumentation = mock(classOf[Instrumentation])

    intercept[RuntimeException] {
      kamonInstrumentation.register(fakeInstrumentation)
    }
  }

  class ConnectionInstrumentationPrototype extends KamonInstrumentation {

    forSubtypeOf2("java.sql.Connection", (builder:InstrumentationDescription.Builder) =>{
      builder.build()
    }
    )

//    addMixin(classOf[MixinTest])

    //    addTransformation { (builder: DynamicType.Builder[_], _: TypeDescription) ⇒
    //      builder
    //        .method(named("prepareStatement").and(TakesArguments))
    //        .intercept(to(ConnectionInterceptor).filter(NotDeclaredByObject))
    //    }
  }
}

object ConnectionInterceptor {
  @RuntimeType
  @throws[SQLException]
  def prepareStatement(@SuperCall callable: Callable[PreparedStatementExtension], @Argument(0) sql: String): Any = {
    val prepareStatement = callable.call()
    prepareStatement.setSql(sql)
    prepareStatement
  }
}

class MixinTest extends Serializable {
  var a: String = _

  @Initializer
  def init() = this.a = "Pepe"
}

trait PreparedStatementExtension {
  def getSql: String
  def setSql(sql: String): Unit
}