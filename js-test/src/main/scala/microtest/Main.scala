package microtest

import TestExecutionContext.value
import microtest.framework.TestSuite._
import microtest.framework.TestSuite
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Try, Failure, Success}
import scala.collection.generic.CanBuildFrom
import java.io.{StringWriter, PrintWriter}

object Main {
  def main(args: Array[String]): Unit = {
    val formatter = new DefaultFormatter(color = true, truncate=500)
//
    println(formatter.format(Core.tests.run()))
//    println(formatter.format(Nesting.tests.run(testPath=Seq("helloWorld"))))
  }

}

