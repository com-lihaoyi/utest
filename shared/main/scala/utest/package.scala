import scala.reflect.ClassTag
import scala.reflect.macros.Context
import scala.util.{Failure, Success, Try}
import utest.framework.{TestTreeSeq, Test}
import utest.asserts._
import utest.util.Tree
import concurrent.duration._
import utest.util.Tree

/**
 * Created by haoyi on 1/24/14.
 */
package object utest {
  implicit val retryInterval = new RetryInterval(100.millis)
  implicit val retryMax = new RetryMax(1.second)
  import language.experimental.macros

  /**
   * Checks that one or more expressions are true; otherwises raises an
   * exception with some debugging info
   */
  def assert(exprs: Boolean*): Unit = macro Asserts.assertProxy
  /**
   * Checks that one or more expressions all become true within a certain
   * period of time. Polls at a regular interval to check this.
   */
  def eventually(exprs: Boolean*): Unit = macro Concurrent.eventuallyProxy
  /**
   * Checks that one or more expressions all remain true within a certain
   * period of time. Polls at a regular interval to check this.
   */
  def continually(exprs: Boolean*): Unit = macro Concurrent.continuallyProxy

  /**
   * Asserts that the given value matches the PartialFunction. Useful for using
   * pattern matching to validate the shape of a data structure.
   */
  def assertMatch[T](t: T)(pf: PartialFunction[T, Unit]): Unit = {
    if (pf.isDefinedAt(t)) pf(t)
    else throw new java.lang.AssertionError("Matching failed " + t)
  }

  /**
   * Asserts that the given block raises the expected exception. The exception
   * is returned if raised, and an `AssertionError` is raised if the expected
   * exception does not appear.
   */
  def intercept[T: ClassTag](exprs: Unit): T = macro Asserts.interceptProxy[T]

  /**
   * Extension methods to allow you to create tests via the "omg"-{ ... }
   * syntax.
   */
  implicit class TestableString(s: String){
    /**
     * Used to demarcate tests with the `TestSuite{ ... }` block. Has no
     * meaning outside that block
     */
    def -(x: => Any) = ???
  }

  /**
   * Extension methods on `Tree[Test]` in order to conveniently run the tests
   * and aggregate the results
   */
  implicit def toTestSeq(t: Tree[Test]) = new TestTreeSeq(t)

  val ClassTag = scala.reflect.ClassTag



  val TestSuite = framework.TestSuite
  type TestSuite  = framework.TestSuite
}

