
import utest.asserts._
import scala.concurrent.duration._

/**
 * Created by haoyi on 1/24/14.
 */
package object utest extends utest.asserts.Asserts{

  implicit val retryInterval = new RetryInterval(100.millis)
  implicit val retryMax = new RetryMax(1.second)

  type Show = asserts.Show
  /**
   * Extension methods to allow you to create tests via the "omg"-{ ... }
   * syntax.
   */
  @reflect.internal.annotations.compileTimeOnly("String#- method should only be used directly inside a Tests{} macro")
  implicit class TestableString(s: String){
    /**
     * Used to demarcate tests with the `TestSuite{ ... }` block. Has no
     * meaning outside that block
     */
    @deprecated("Use the test(\"foo\") - {...} syntax instead")
    @reflect.internal.annotations.compileTimeOnly("String#- method should only be used directly inside a Tests{} macro")
    def -(x: => Any) = ()
  }

  @reflect.internal.annotations.compileTimeOnly("Symbol#- method should only be used directly inside a Tests{} macro")
  implicit class TestableSymbol(s: Symbol){
    /**
     * Used to demarcate tests with the `TestSuite{ ... }` block. Has no
     * meaning outside that block
     */
    @deprecated("Use the test(\"foo\"){...} syntax instead")
    @reflect.internal.annotations.compileTimeOnly("Symbol#apply method should only be used directly inside a Tests{} macro")
    def apply(x: => Any) = ()
    /**
     * Used to demarcate tests with the `TestSuite{ ... }` block. Has no
     * meaning outside that block
     */
    @deprecated("Use the test(\"foo\") - {...} syntax instead")
    @reflect.internal.annotations.compileTimeOnly("Symbol#- method should only be used directly inside a Tests{} macro")
    def -(x: => Any) = ()
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
    @reflect.internal.annotations.compileTimeOnly("test - method should only be used directly inside a Tests{} macro")
    def -(x: => Any) = ()

    @reflect.internal.annotations.compileTimeOnly("test{} method should only be used directly inside a Tests{} macro")
    def apply(x: => Any) = ()

    def apply(name: String) = Apply(name)
    case class Apply(name: String){
      @reflect.internal.annotations.compileTimeOnly("test() -  method should only be used directly inside a Tests{} macro")
      def -(x: => Any) = ()

      @reflect.internal.annotations.compileTimeOnly("test()() method should only be used directly inside a Tests{} macro")
      def apply(x: => Any) = ()
    }


//    @reflect.internal.annotations.compileTimeOnly("test()() method should only be used directly inside a Tests{} macro")
//    def apply(x: => Any) = ()
  }


}

