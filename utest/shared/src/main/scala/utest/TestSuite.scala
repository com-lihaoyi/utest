package utest

//import acyclic.file
import utest.asserts.DummyTypeclass
import utest.framework.Formatter
import scala.concurrent.{Future, ExecutionContext}
import scala.reflect.macros.Context
import scala.language.experimental.macros

import scala.scalajs.js.annotation.JSExportDescendentObjects

/**
 * Marker class used to mark an `object` as something containing tests. Used
 * for test-discovery by SBT.
 */
@JSExportDescendentObjects
abstract class TestSuite
  extends TestSuiteMacro
  with utest.asserts.Asserts[DummyTypeclass]
  with Formatter{

  def utestWrap(runBody: => concurrent.Future[Any])
               (implicit ec: ExecutionContext): concurrent.Future[Any] = {
    runBody
  }

  def utestTruncateLength = 5000
  override def formatTruncate = utestTruncateLength
  def assertPrettyPrint[T: DummyTypeclass](t: T) = t.toString.take(utestTruncateLength)

  /**
   * The tests within this `object`.
   */
  def tests: framework.Tree[framework.Test]
}

trait TestSuiteMacro{
  /**
    * Macro to demarcate a `Tree[Test]`.
    */
  def apply(expr: Unit): framework.Tree[framework.Test] = macro framework.TreeBuilder.applyImpl
}
object TestSuite extends TestSuiteMacro{


  trait Retries extends utest.TestSuite{
    val utestRetryCount: Int
    override def utestWrap(body: => Future[Any])(implicit ec: ExecutionContext): Future[Any] = {
      def rec(count: Int): Future[Any] = {
        body.recoverWith { case e =>
          if (count < 5) rec(count + 1)
          else throw e
        }
      }
      val res = rec(0)
      res
    }
  }
}
