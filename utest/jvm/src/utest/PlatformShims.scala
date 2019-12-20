package utest

import scala.concurrent.{Await, Future}
import concurrent.duration._

/**
 * Platform specific stuff that differs between JVM and JS
 */
object PlatformShims extends PlatformShimsVersionSpecific {
  def await[T](f: Future[T]): T = Await.result(f, 10.hours)

  def loadModule(name: String, loader: ClassLoader): Any =
    Reflect
      .lookupLoadableModuleClass(name + "$", loader)
      .getOrElse(throw new ClassNotFoundException(name))
      .loadModule()
}
