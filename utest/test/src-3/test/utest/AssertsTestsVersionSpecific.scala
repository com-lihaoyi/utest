package test.utest
import utest._

object AssertsTestsVersionSpecific extends utest.TestSuite{


  def tests = Tests{
    test("assertCompileError"){
      test("failure"){
        test - {
          try assertCompileError("1 + 1").check("")
          catch { case e: utest.AssertionError =>
            assert(e.getMessage ==
              """assertCompileError check failed to have a compilation error when compiling
                |1 + 1""".stripMargin)
          }
        }

        test - {
          try {
            val x = 0
            assertCompileError("x + x").check("")
          }
          catch { case e: utest.AssertionError =>
            assert(e.getMessage ==
              """assertCompileError check failed to have a compilation error when compiling
                |x + x""".stripMargin)
          }
        }

        test - assertCompileError(
            """assertCompileError("1" * 2).check("")"""
          ).check("""assertCompileError("1" * 2).check("")
                      |                   ^""".stripMargin,
              """argument to compileError must be a statically known String but was: augmentString("1").*(2)"""
          )
      }
    }
  }
}
