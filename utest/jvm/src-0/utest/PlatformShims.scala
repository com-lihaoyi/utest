package utest

import scala.concurrent.{Await, Future}
import concurrent.duration._

/**
 * Platform specific stuff that differs between JVM and JS
 */
object PlatformShims {
  def await[T](f: Future[T]): T = Await.result(f, 10.hours)

  type PortableScalaReflectExcerpts =
    utest.EnableReflectiveInstantiation

  def loadModule(name: String, loader: ClassLoader): Any =
    PortableScalaReflectExcerpts
      .lookupLoadableModuleClass(name + "$", loader)
      .getOrElse(throw new ClassNotFoundException(name))
      .loadModule()

}
