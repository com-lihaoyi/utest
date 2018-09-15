package utest

import scala.concurrent.Future
import org.portablescala.reflect.Reflect

/**
 * Platform specific stuff that differs between JVM and JS
 */
object PlatformShims {
  def await[T](f: Future[T]): T = {
    f.value match {
      case Some(v) => v.get
      case None => throw new IllegalStateException(
        "Test that returns Future must be run asynchronously in Scala.js, see TestTreeSeq::runAsync"
      )
    }
  }

  type EnableReflectiveInstantiation =
    org.portablescala.reflect.annotation.EnableReflectiveInstantiation

  def loadModule(name: String, loader: ClassLoader): Any = {
    Reflect
      .lookupLoadableModuleClass(name + "$", loader)
      .getOrElse(throw new ClassNotFoundException(name))
      .loadModule()
  }
}
