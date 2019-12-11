package test.utest
import utest._

object AssertsTestsVersionSpecific extends utest.TestSuite{


  def tests = Tests{
    test("compileError"){
      test("failure"){
        // Use compileError to check itself to verify that when it
        // doesn't throw an error, it actually does (super meta!)
        * - compileError("""
            compileError("1 + 1").check(
              ""
            )
          """).check(
          """
            compileError("1 + 1").check(
                        ^
          """,
          "compileError check failed to have a compilation error"
        )
        * - compileError("""
            val x = 0
            compileError("x + x").check(
            ""
          )
          """).check(
          """
            compileError("x + x").check(
                        ^
          """,
          "compileError check failed to have a compilation error"
        )
        * - compileError("""
            compileError("1" * 2).check(
              ""
            )
        """).check(
          """
            compileError("1" * 2).check(
                             ^
          """,
          "You can only have literal strings in compileError"
        )

      }
      test("compileTimeOnly"){
        // Make sure that when the body contains a `@compileTimeOnly`, it
        // gets counted as a valid compile error and `compileError` passes
        compileError("compileTimeOnlyVal").check(
          """
        compileError("compileTimeOnlyVal").check(
                      ^
          """,
          "compileTimeOnlyVal should be a compile error if used!"
        )

        compileError("{ println(1 + 1); class F{ def foo() = { println(compileTimeOnlyVal) } } }").check(
          """
        compileError("{ println(1 + 1); class F{ def foo() = { println(compileTimeOnlyVal) } } }").check(
                                                                       ^
          """,
          "compileTimeOnlyVal should be a compile error if used!"
        )
      }
    }
  }

  @annotation.compileTimeOnly(
    "compileTimeOnlyVal should be a compile error if used!"
  )
  def compileTimeOnlyVal = 1
}

