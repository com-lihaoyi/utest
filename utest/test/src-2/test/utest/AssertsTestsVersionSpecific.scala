package test.utest
import utest._

object AssertsTestsVersionSpecific extends utest.TestSuite{

  implicit val colors = shaded.pprint.TPrintColors.Colors
  def tests = Tests{
    test("assertCompileError"){
      test("failure"){
        // Use assertCompileError to check itself to verify that when it
        // doesn't throw an error, it actually does (super meta!)
        test("1") {
          assertCompileError("""
              assertCompileError("1 + 1").check(
                ""
              )
            """).check(
            """
              assertCompileError("1 + 1").check(
                          ^
            """,
            "assertCompileError check failed to have a compilation error"
          )
        }
        test("2") {
          assertCompileError("""
              val x = 0
              assertCompileError("x + x").check(
              ""
            )
            """).check(
            """
              assertCompileError("x + x").check(
                          ^
            """,
            "assertCompileError check failed to have a compilation error"
          )
        }
        test("3") {
          assertCompileError("""
              assertCompileError("1" * 2).check(
                ""
              )
          """).check(
            """
              assertCompileError("1" * 2).check(
                               ^
            """,
            "You can only have literal strings in assertCompileError"
          )
        }

      }
      test("compileTimeOnly"){
        // Make sure that when the body contains a `@compileTimeOnly`, it
        // gets counted as a valid compile error and `assertCompileError` passes
        assertCompileError("compileTimeOnlyVal").check(
          """
        assertCompileError("compileTimeOnlyVal").check(
                      ^
          """,
          "compileTimeOnlyVal should be a compile error if used!"
        )

        assertCompileError("{ println(1 + 1); class F{ def foo() = { println(compileTimeOnlyVal) } } }").check(
          """
        assertCompileError("{ println(1 + 1); class F{ def foo() = { println(compileTimeOnlyVal) } } }").check(
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
          TestValue.Single("x", Some(shaded.pprint.tprint[Int]), 1),
          TestValue.Single("iAmCow", Some(shaded.pprint.tprint[Seq[String]]), Seq("2.0")))
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
        Predef.assert(e.captured == Seq(TestValue.Single("a", Some(shaded.pprint.tprint[Iterator[Nothing]]), Iterator.empty)))
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

