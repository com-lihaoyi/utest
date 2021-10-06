package test.utest
import utest._

object AssertsTestsVersionSpecific extends utest.TestSuite{


  def tests = Tests{
    test("compileError"){
      test("failure"){
        // Use compileError to check itself to verify that when it
        // doesn't throw an error, it actually does (super meta!)
        test("1") {
          compileError("""
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
        }
        test("2") {
          compileError("""
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
        }
        test("3") {
          compileError("""
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
    test("failure"){
      try {
        val x = 1
        val iAmCow = Seq("2.0")
        assertMatch(Seq(x, iAmCow, 3)){case Seq(1, 2) =>}
        Predef.assert(false)
      } catch{ case e: utest.AssertionError =>

        Predef.assert(e.captured == Seq(
          TestValue("x", "Int", 1), TestValue("iAmCow", "Seq[String]", Seq("2.0")))
        )
        Predef.assert(e.getMessage.contains("assertMatch(Seq(x, iAmCow, 3)){case Seq(1, 2) =>}"))

        Predef.assert(e.getCause().getMessage.contains("List(1, List(2.0), 3)"))
        e.getMessage
      }
    }

    test("failureWithException"){
      try {
        val a = Iterator.empty
        val b = 2
        assertMatch(Seq(a.next(), 3, b)){case Seq(1, 2) =>}
        Predef.assert(false)
      } catch{ case e: utest.AssertionError =>
        Predef.assert(e.captured == Seq(TestValue("a", "Iterator[Nothing]", Iterator.empty)))
        Predef.assert(e.cause.isInstanceOf[NoSuchElementException])
        Predef.assert(e.getMessage.contains("assertMatch(Seq(a.next(), 3, b)){case Seq(1, 2) =>}"))
        e.getMessage
      }
    }
  }

  @annotation.compileTimeOnly(
    "compileTimeOnlyVal should be a compile error if used!"
  )
  def compileTimeOnlyVal = 1
}

