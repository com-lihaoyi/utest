package utest

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.scalajs.js.annotation.{JSExportDescendentObjects, JSExport}
import scala.scalajs.js

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
    e.getStackTraceString
  }
  @JSExport
  def runSuite(suite: TestSuite,
               path: js.Array[String],
               args: js.Array[String]) = {
    println("PlatformShims.runSuite")
    println(suite)
    println(path)
    println(args)
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
