package utest

//import acyclic.file


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
  extends asserts.Asserts
  with framework.Formatter
  with framework.Executor{
  def tests: framework.Tests
  @deprecated("Use `utest.Tests{...}` instead")
  def apply(expr: Unit): framework.Tests = macro framework.TestHierarchyBuilder.applyImpl
}

object TestSuite {

  @deprecated("Use `utest.Tests{...}` instead")
  def apply(expr: Unit): framework.Tests = macro framework.TestHierarchyBuilder.applyImpl
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


