package utest

// Taken from the implementation for JS

import scala.concurrent.Future

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
}
