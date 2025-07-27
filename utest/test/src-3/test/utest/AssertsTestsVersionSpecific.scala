package test.utest
import utest._

object AssertsTestsVersionSpecific extends utest.TestSuite{


  def tests = Tests{
    test("compileError"){
      test("failure"){
        test - {
          try compileError("1 + 1").check("")
          catch { case e: utest.AssertionError =>
            assert(e.getMessage ==
              """compileError check failed to have a compilation error when compiling
                |1 + 1""".stripMargin)
          }
        }

        test - {
          try {
            val x = 0
            compileError("x + x").check("")
          }
          catch { case e: utest.AssertionError =>
            assert(e.getMessage ==
              """compileError check failed to have a compilation error when compiling
                |x + x""".stripMargin)
          }
        }

        test - compileError(
            """compileError("1" * 2).check("")"""
          ).check("""compileError("1" * 2).check("")
                      |             ^""".stripMargin,
              """argument to compileError must be a statically known String but was: augmentString("1").*(2)"""
          )
      }
    }
  }
}

