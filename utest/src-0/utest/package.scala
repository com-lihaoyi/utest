
import utest.asserts._
import scala.concurrent.duration._

/**
 * Created by haoyi on 1/24/14.
 */
package object utest extends utest.asserts.Asserts{

  implicit val retryInterval: RetryInterval = new RetryInterval(100.millis)
  implicit val retryMax: RetryMax = new RetryMax(1.second)

  type Show = asserts.Show
  /**
   * Extension methods to allow you to create tests via the "omg"-{ ... }
   * syntax.
   */
  implicit class TestableString(s: String){
    // def apply(foo: String)(body: Any) = ???

    /**
     * Used to demarcate tests with the `TestSuite{ ... }` block. Has no
     * meaning outside that block
     */
    @deprecated("Use the test(\"foo\") - {...} syntax instead")
    erased def -(x: => Any) = ()
  }

  implicit class TestableSymbol(s: Symbol){
    /**
     * Used to demarcate tests with the `TestSuite{ ... }` block. Has no
     * meaning outside that block
     */
    @deprecated("Use the test(\"foo\"){...} syntax instead")
    erased def apply(x: => Any) = ()
    /**
     * Used to demarcate tests with the `TestSuite{ ... }` block. Has no
     * meaning outside that block
     */
    @deprecated("Use the test(\"foo\") - {...} syntax instead")
    erased def -(x: => Any) = ()
  }

  /**
   * Placeholder object used to declare test cases which you don't want to 
   * bother naming. These test cases are named with sequential numbers 
   * starting from "0", "1", "2", etc.
   */
  object * { 
    /**
     * Declares a numbered test-case
     */
    @deprecated("Use the test - {...} syntax instead")
    def -(x: => Any) = ()
  }

  object test{
    erased def -(x: => Any) = ()

    erased def apply(x: => Any) = ()

    def apply(name: String) = Apply(name)
    case class Apply(name: String){
      erased def -(x: => Any) = ()

      erased def apply(x: => Any) = ()
    }


    // def apply(x: => Any) = ()
  }


}

