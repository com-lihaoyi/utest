package utest

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.scalajs.js.annotation.{JSExportDescendentObjects, JSExport}
import scala.scalajs.js
import scala.scalajs.runtime.StackTrace.ColumnStackTraceElement

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

  def printTrace(e: Throwable): Unit = {
    println(
      e.getStackTrace
       .map(s => s"XXSecretXX/trace/${s.getClassName}/${s.getMethodName}/${s.getFileName}/${s.getLineNumber}/${s.getColumnNumber}")
       .mkString("\n")
    )
  }

  @JSExport
  def runSuite(suite: TestSuite,
               path: js.Array[String],
               args: js.Array[String]) = {
    val res = utest.runSuite(
      suite,
      path,
      args,
      s => println("XXSecretXX/addCount/" + s),
      s => println("XXSecretXX/log/" + s),
      s => println("XXSecretXX/addTotal/" + s)
    )
    println("XXSecretXX/result/" + res.replace("\n", "ZZZZ"))
  }
  @JSExportDescendentObjects
  class Test
}
