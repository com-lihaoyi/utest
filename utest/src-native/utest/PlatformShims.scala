package utest

// Taken from the implementation for JS

import scala.concurrent.Future
import scala.scalanative.reflect.Reflect

/**
 * Platform specific stuff that differs between JVM, JS and Native
 */
object PlatformShims {
  def await[T](f: Future[T]): T = {
    scala.scalanative.runtime.loop()
    f.value match {
      case Some(v) => v.get
      case None => throw new IllegalStateException(
        "Test that returns Future must be run asynchronously in Scala Native, see TestTreeSeq::runAsync"
      )
    }
  }

  type EnableReflectiveInstantiation =
    scala.scalanative.reflect.annotation.EnableReflectiveInstantiation

  def loadModule(name: String, loader: ClassLoader): Any = {
    Reflect
      .lookupLoadableModuleClass(name + "$")
      .getOrElse(throw new ClassNotFoundException(name))
      .loadModule()
  }
}
