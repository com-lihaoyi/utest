package utest

// Taken from the implementation for JS

import scala.concurrent.{Await, Future}
import concurrent.duration._
import scala.scalanative.reflect.Reflect

/**
 * Platform specific stuff that differs between JVM, JS and Native
 */
object PlatformShims {
  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)

  type EnableReflectiveInstantiation =
    scala.scalanative.reflect.annotation.EnableReflectiveInstantiation

  def loadModule(name: String, loader: ClassLoader): Any = {
    Reflect
      .lookupLoadableModuleClass(name + "$")
      .getOrElse(throw new ClassNotFoundException(name))
      .loadModule()
  }
}
