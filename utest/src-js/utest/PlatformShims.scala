package utest

import scala.concurrent.Future
import org.portablescala.reflect.Reflect

/**
 * Platform specific stuff that differs between JVM, JS and Native
 */
object PlatformShims {
  def await[T](f: Future[T]): T = {
    f.value match {
      case Some(v) => v.get
      case None => throw new IllegalStateException(
        "Test that returns Future must be run asynchronously in Scala.js, see TestCallTree::runAsync"
      )
    }
  }

  type EnableReflectiveInstantiation =
    org.portablescala.reflect.annotation.EnableReflectiveInstantiation

  def loadModule(name: String, loader: ClassLoader): Any = {
    Reflect
      .lookupLoadableModuleClass(name + "$", loader)
      .map(_.loadModule())
      .orElse(Reflect.lookupInstantiatableClass(name, loader).map(_.newInstance()))
      .getOrElse(throw new ClassNotFoundException(name))
  }
}
