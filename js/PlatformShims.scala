package utest


import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportDescendentObjects}
import scala.scalajs.runtime.StackTrace.ColumnStackTraceElement
import scala.util.{Failure, Success}

/**
 * Platform specific stuff that differs between JVM and JS
 */
@JSExport
object PlatformShims {
  def await[T](f: Future[T]): T = {
    f.value match {
      case Some(v) => v.get
      case None => throw new IllegalStateException(
        "Test that returns Future must be run asynchronously in Scala.js, see TestTreeSeq::runAsync"
      )
    }
  }
}
