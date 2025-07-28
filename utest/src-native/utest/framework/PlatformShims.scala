package utest.framework

// Taken from the implementation for JS

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
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
      .map(_.loadModule())
      .orElse(Reflect.lookupInstantiatableClass(name).map(_.newInstance()))
      .getOrElse(throw new ClassNotFoundException(name))
  }
}
