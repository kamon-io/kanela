package utils

import java.lang.instrument.{ ClassDefinition, ClassFileTransformer, Instrumentation }
import java.util.jar.JarFile

object FakeInstrumentation extends Instrumentation {
  override def appendToBootstrapClassLoaderSearch(jarfile: JarFile): Unit = Unit

  override def isRetransformClassesSupported: Boolean = true

  override def retransformClasses(classes: Class[_]*): Unit = Unit

  override def isModifiableClass(theClass: Class[_]): Boolean = true

  override def getObjectSize(objectToSize: scala.Any): Long = 0

  override def removeTransformer(transformer: ClassFileTransformer): Boolean = true

  override def isNativeMethodPrefixSupported: Boolean = true

  override def getInitiatedClasses(loader: ClassLoader): Array[Class[_]] = Array()

  override def getAllLoadedClasses: Array[Class[_]] = Array()

  override def appendToSystemClassLoaderSearch(jarfile: JarFile): Unit = Unit

  override def redefineClasses(definitions: ClassDefinition*): Unit = Unit

  override def setNativeMethodPrefix(transformer: ClassFileTransformer, prefix: String): Unit = Unit

  override def isRedefineClassesSupported: Boolean = true

  override def addTransformer(transformer: ClassFileTransformer, canRetransform: Boolean): Unit = Unit

  override def addTransformer(transformer: ClassFileTransformer): Unit = Unit
}
