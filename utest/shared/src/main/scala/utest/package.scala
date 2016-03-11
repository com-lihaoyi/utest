

/**
 * Created by haoyi on 1/24/14.
 */
package object utest {

  @reflect.internal.annotations.compileTimeOnly("String#- method should only be used directly inside a TestSuite{} macro")
  implicit class TestableSymbol(s: Symbol){
    /**
     * Used to demarcate tests with the `TestSuite{ ... }` block. Has no
     * meaning outside that block
     */
    @reflect.internal.annotations.compileTimeOnly("Symbol#apply method should only be used directly inside a TestSuite{} macro")
    def apply(x: => Any) = ()

  }

  /**
    * Just here to bootstrap utest until I get acyclic published for 2.12.0
    */
  private[utest] object acyclic{
    val file = ()
  }
}

