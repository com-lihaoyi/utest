package utest

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.scalajs.js.annotation.{JSExportDescendentObjects, JSExport}
import scala.scalajs.js
import scala.scalajs.runtime.StackTrace.ColumnStackTraceElement

/**
 * Platform specific stuff that differs between JVM and JS
 */
@JSExport
object PlatformShims {
  def await[T](f: Future[T]): T = f.value.get.get

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
    println("XXSecretXX/result/" + res.replace("\\", "\\\\").replace("\n", "\\n"))
  }

  @JSExportDescendentObjects
  class Test

  def globalExecutionContext: ExecutionContext = {
    throw new Exception("global ExecutionContext doesn't exist in Scala.js")
  }
}