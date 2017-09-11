package utest

//import acyclic.file

import utest.framework.{Formatter, TestHierarchy}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

/**
 * Marker class used to mark an `object` as something containing tests. Used
 * for test-discovery by SBT.
 */
@EnableReflectiveInstantiation
abstract class TestSuite
  extends TestSuiteMacro
  with utest.asserts.Asserts
  with Formatter{

  def utestWrap(path: Seq[String], runBody: => concurrent.Future[Any])
               (implicit ec: ExecutionContext): concurrent.Future[Any] = {
    runBody
  }

  /**
   * The tests within this `object`.
   */
  def tests: TestHierarchy
}

trait TestSuiteMacro{
  /**
    * Macro to demarcate a `Tree[Test]`.
    */
  def apply(expr: Unit): TestHierarchy = macro framework.TestHierarchyBuilder.applyImpl
}
object TestSuite extends TestSuiteMacro{


  trait Retries extends utest.TestSuite{
    val utestRetryCount: Int
    override def utestWrap(path: Seq[String], body: => Future[Any])(implicit ec: ExecutionContext): Future[Any] = {
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


