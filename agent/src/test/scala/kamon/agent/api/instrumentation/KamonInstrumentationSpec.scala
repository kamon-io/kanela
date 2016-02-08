package kamon.agent.api.instrumentation

import java.sql.SQLException
import java.util.concurrent.Callable
import java.util.function.{ BiFunction ⇒ JBifunction, Supplier ⇒ JSupplier }
import javaslang.{ Function2 ⇒ JFunction2 }

import kamon.api.instrumentation.KamonInstrumentation
import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.MethodDelegation.to
import net.bytebuddy.implementation.bind.annotation.{ Argument, RuntimeType, SuperCall }
import net.bytebuddy.matcher.ElementMatchers._

class KamonInstrumentationSpec extends KamonInstrumentation {

  implicit def toJavaSupplier[A](f: ⇒ A): JSupplier[A] = new JSupplier[A] {
    override def get(): A = f
  }

  implicit def toJavaFunction2[A, B, C](f: (A, B) ⇒ C): JFunction2[A, B, C] = new JFunction2[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  implicit def toJavaBiFunction[A, B, C](f: (A, B) ⇒ C): JBifunction[A, B, C] = new JBifunction[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  forSubtypeOf("java.sql.Connection")

  var a = (builder: DynamicType.Builder[_], t: TypeDescription) ⇒ {
    builder
      .method(named("prepareStatement").and(KamonInstrumentation.NotTakesArguments))
      .intercept(to(ConnectionInterceptor).filter(KamonInstrumentation.NotDeclaredByObject))
  }

  def addTransformation(builder: DynamicType.Builder[_], t: TypeDescription) =
    builder
      .method(named("prepareStatement").and(takesArguments(0)))
      .intercept(to(ConnectionInterceptor).filter(KamonInstrumentation.NotDeclaredByObject))

  object ConnectionInterceptor {
    @RuntimeType
    @throws[SQLException]
    def prepareStatement(@SuperCall callable: Callable[PreparedStatementExtension], @Argument(0) sql: String): Any = {
      val prepareStatement = callable.call()
      prepareStatement.setSql(sql)
      prepareStatement
    }

  }

  trait PreparedStatementExtension {
    def getSql: String
    def setSql(sql: String): Unit
  }
}
