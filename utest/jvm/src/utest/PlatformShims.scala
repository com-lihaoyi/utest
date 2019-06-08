package utest

import scala.concurrent.{Await, Future}
import concurrent.duration._
import org.portablescala.reflect.Reflect

/**
 * Platform specific stuff that differs between JVM and JS
 */
object PlatformShims {
  def await[T](f: Future[T]): T = Await.result(f, 10.hours)

  type EnableReflectiveInstantiation =
    org.portablescala.reflect.annotation.EnableReflectiveInstantiation

  def loadModule(name: String, loader: ClassLoader): Any = {
    Reflect
      .lookupLoadableModuleClass(name + "$", loader)
      .getOrElse(throw new ClassNotFoundException(name))
      .loadModule()
  }
}
