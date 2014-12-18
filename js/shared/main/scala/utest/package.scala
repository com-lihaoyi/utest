
import utest.asserts._
import utest.framework.{Test, TestTreeSeq}
import utest.util.Tree

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.Failure

/**
 * Created by haoyi on 1/24/14.
 */
package object utest {
  implicit val retryInterval = new RetryInterval(100.millis)
  implicit val retryMax = new RetryMax(1.second)
  import scala.language.experimental.macros


  /**
   * Asserts that the given expression fails to compile, and returns a
   * [[CompileError]] containing the message of the failure. If the expression
   * compile successfully, this macro itself will raise a compilation error.
   */
  def compileError(expr: String): CompileError = macro Asserts.compileError
  /**
   * Checks that one or more expressions are true; otherwises raises an
   * exception with some debugging info
   */
  def assert(exprs: Boolean*): Unit = macro Asserts.assertProxy
  /**
   * Checks that one or more expressions all become true within a certain
   * period of time. Polls at a regular interval to check this.
   */
  def eventually(exprs: Boolean*): Unit = macro Parallel.eventuallyProxy
  /**
   * Checks that one or more expressions all remain true within a certain
   * period of time. Polls at a regular interval to check this.
   */
  def continually(exprs: Boolean*): Unit = macro Parallel.continuallyProxy

  /**
   * Asserts that the given value matches the PartialFunction. Useful for using
   * pattern matching to validate the shape of a data structure.
   */
  def assertMatch(t: Any)(pf: PartialFunction[Any, Unit]): Unit =  macro Asserts.assertMatchProxy


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

  implicit class TestableSymbol(s: Symbol){
    /**
     * Used to demarcate tests with the `TestSuite{ ... }` block. Has no
     * meaning outside that block
     */
    def apply(x: => Any) = ???
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

  val TestSuite = framework.TestSuite
  type TestSuite  = framework.TestSuite
  
  /**
   * Placeholder object used to declare test cases which you don't want to 
   * bother naming. These test cases are named with sequential numbers 
   * starting from "0", "1", "2", etc.
   */
  object * { 
    /**
     * Declares a numbered test-case
     */
    def -(x: => Any) = ???
  }
  def runSuite(suite: TestSuite,
               path: Array[String],
               args: Array[String],
               addCount: String => Unit,
               log: String => Unit,
               logFailure: (String, Throwable) => Unit,
               addTotal: String => Unit): Future[String] = {
    import suite.tests
    val (indices, found) = tests.resolve(path)
    addTotal(found.length.toString)

    implicit val ec =
      if (utest.util.ArgParse.find("--parallel", _.toBoolean, false, true)(args)){
        scala.concurrent.ExecutionContext.global
      }else{
        ExecutionContext.RunNow
      }

    val formatAll = utest.util.ArgParse.find("--formatAll", _.toBoolean, false, true)(args)
    val formatter = DefaultFormatter(args)
    val results = tests.runAsync(
      (subpath, s) => {
        addCount(s.value.isSuccess.toString)
        val str = formatter.formatSingle(path ++ subpath, s)
        log(str)
        val trace = utest.util.ArgParse.find("--trace", _.toBoolean, true, true)(args)
        s.value match{
          case Failure(e) => logFailure(str, e)
          case _ => ()
        }
      },
      testPath = path
    )(ec)

    results.map(formatter.format)
  }
}

