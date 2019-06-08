package utest

// Taken from the implementation for JS

import scala.concurrent.Future
import org.scalajs.testinterface.TestUtils

/**
 * Platform specific stuff that differs between JVM and Native
 */
object PlatformShims {
  def await[T](f: Future[T]): T = {
    f.value match {
      case Some(v) => v.get
      case None => throw new IllegalStateException(
        "Test that returns Future must be run asynchronously in Scala Native, see TestTreeSeq::runAsync"
      )
    }
  }

  type EnableReflectiveInstantiation =
    scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

  def loadModule(name: String, loader: ClassLoader): Any =
    TestUtils.loadModule(name, loader)
}
