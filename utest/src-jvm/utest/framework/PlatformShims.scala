package utest.framework

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

/**
 * Platform specific stuff that differs between JVM, JS and Native
 */
object PlatformShims extends PlatformShimsVersionSpecific {
  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)

  def loadModule(name: String, loader: ClassLoader): Any =
    Reflect
      .lookupLoadableModuleClass(name + "$", loader)
      .map(_.loadModule())
      .orElse(Reflect.lookupInstantiatableClass(name, loader).map(_.newInstance()))
      .getOrElse(throw new ClassNotFoundException(name))
}
