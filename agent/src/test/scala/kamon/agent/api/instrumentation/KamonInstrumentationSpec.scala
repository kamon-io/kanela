package kamon.agent.api.instrumentation

import java.lang.instrument.{ ClassDefinition, ClassFileTransformer, Instrumentation }
import java.sql.SQLException
import java.util.concurrent.Callable
import java.util.function.{ BiFunction ⇒ JBifunction, Supplier ⇒ JSupplier }
import java.util.jar.JarFile
import javaslang.{ Function2 ⇒ JFunction2 }

import kamon.api.instrumentation.KamonInstrumentation
import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.MethodDelegation.to
import net.bytebuddy.implementation.bind.annotation.{ Argument, RuntimeType, SuperCall }
import net.bytebuddy.matcher.ElementMatchers._
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

class KamonInstrumentationSpec extends WordSpecLike with Matchers with BeforeAndAfterAll {

  implicit def toJavaSupplier[A](f: ⇒ A): JSupplier[A] = new JSupplier[A] {
    override def get(): A = f
  }

  implicit def toJavaFunction2[A, B, C](f: (A, B) ⇒ C): JFunction2[A, B, C] = new JFunction2[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  implicit def toJavaBiFunction[A, B, C](f: (A, B) ⇒ C): JBifunction[A, B, C] = new JBifunction[A, B, C] {
    override def apply(a: A, b: B): C = f(a, b)
  }

  "should validate that there is an element selected by elementMatcher" in {
    val kamonInstrumentation = new KamonInstrumentation() {}

    intercept[RuntimeException] {
      kamonInstrumentation.register(FakeInstrumentation)
    }
  }

  class ConnectionInstrumentationPrototype extends KamonInstrumentation() {

    forSubtypeOf("java.sql.Connection")

    addTransformation { (builder: DynamicType.Builder[_], _: TypeDescription) ⇒
      builder
        .method(named("prepareStatement").and(KamonInstrumentation.NotTakesArguments))
        .intercept(to(ConnectionInterceptor).filter(KamonInstrumentation.NotDeclaredByObject))
    }
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

trait PreparedStatementExtension {
  def getSql: String
  def setSql(sql: String): Unit
}

object FakeInstrumentation extends Instrumentation {
  override def appendToBootstrapClassLoaderSearch(jarfile: JarFile): Unit = ???

  override def isRetransformClassesSupported: Boolean = ???

  override def retransformClasses(classes: Class[_]*): Unit = ???

  override def isModifiableClass(theClass: Class[_]): Boolean = ???

  override def getObjectSize(objectToSize: scala.Any): Long = ???

  override def removeTransformer(transformer: ClassFileTransformer): Boolean = ???

  override def isNativeMethodPrefixSupported: Boolean = ???

  override def getInitiatedClasses(loader: ClassLoader): Array[Class[_]] = ???

  override def getAllLoadedClasses: Array[Class[_]] = ???

  override def appendToSystemClassLoaderSearch(jarfile: JarFile): Unit = ???

  override def redefineClasses(definitions: ClassDefinition*): Unit = ???

  override def setNativeMethodPrefix(transformer: ClassFileTransformer, prefix: String): Unit = ???

  override def isRedefineClassesSupported: Boolean = ???

  override def addTransformer(transformer: ClassFileTransformer, canRetransform: Boolean): Unit = ???

  override def addTransformer(transformer: ClassFileTransformer): Unit = ???
}
