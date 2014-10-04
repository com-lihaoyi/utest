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

  def escape(s: String) = {
    s.replace("\\", "\\\\").replace("\t", "\\t")
  }

  def printTrace(e: Throwable): Unit = {
    def bundle(s: StackTraceElement) = {
      Seq(s.getClassName, s.getMethodName, s.getFileName, s.getLineNumber, s.getColumnNumber)
        .map(_ + "")
        .map(escape)
        .mkString("\t")
    }

    println(
      e.getStackTrace
        .takeWhile(_.getClassName != "utest.framework.TestThunkTree")
        .map(s => s"XXSecretXX/trace/${bundle(s)}")
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
      s => println("XXSecretXX/logFailure/" + s),
      s => println("XXSecretXX/addTotal/" + s)
    )
    res.onComplete {
      case Failure(ex) => Console.err.println(ex.getStackTraceString)
      case Success(results) =>
        println("XXSecretXX/result/" + results.replace("\\", "\\\\").replace("\n", "\\n"))
    }(ExecutionContext.RunNow)
  }

  @JSExportDescendentObjects
  class Test

  def globalExecutionContext: scala.concurrent.ExecutionContext = {
    throw new Exception("global ExecutionContext doesn't exist in Scala.js")
  }
}