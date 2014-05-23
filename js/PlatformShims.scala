package utest

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.scalajs.js.annotation.{JSExportDescendentObjects, JSExport}

/**
 * Platform specific stuff that differs between JVM and JS
 */
@JSExport
object PlatformShims {
  def flatten[T](f: Future[Future[T]]): Future[T] = {
    f.value.get.map(_.value.get) match{
      case Success(Success(v)) => Future.successful(v)
      case Success(Failure(e)) => Future.failed(e)
      case Failure(e)          => Future.failed(e)
    }
  }

  def await[T](f: Future[T]): T = f.value.get.get
  def getTrace(e: Throwable): String = {
    ""
  }
  @JSExport
  def runSuite(suite: TestSuite,
               path: Array[String],
               args: Array[String]) = {
    utest.runSuite(
      suite,
      path,
      args,
      s => println("addCount" + s),
      s => println("log" + s),
      s => println("addTotal" + s))
  }
  @JSExportDescendentObjects
  class Test
}
