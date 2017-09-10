package test.utest
import utest._
import concurrent.ExecutionContext.Implicits.global
object FormatterTests extends utest.TestSuite {
  val tests = this{
    "hello"-{
      var trimmedOutput0: String = ""
      for(i <- 0 until 10) {
        val boa = new java.io.ByteArrayOutputStream()
        val printStream = new java.io.PrintStream(boa)
        val tests = TestSuite {
          'test1 - {
            throw new Exception("test1")
          }
          'test2 - 1

          'test3 - {
            val a = List[Byte](1, 2)
            a(10)
          }
        }
        val results = utest.runWith(
          tests,
          utest.framework.Formatter,
          "MyTestSuite",
          printStream = printStream
        )
        val trimmedOutput = utest.fansi.Str(new String(boa.toByteArray)).plainText.trim
        trimmedOutput0 = trimmedOutput
        val expected =
          """
          |X MyTestSuite.test1 0ms  java.lang.Exception: test1
          |+ MyTestSuite.test2 0ms  1
          |X MyTestSuite.test3 0ms  java.lang.IndexOutOfBoundsException: 10
          |- MyTestSuite
          |  X test1 0ms  java.lang.Exception: test1
          |  + test2 0ms  1
          |  X test3 0ms  java.lang.IndexOutOfBoundsException: 10
        """.stripMargin.trim()

        // Warm up the code a few times first to ensure it finishes in 0ms per test
        if (i == 9) assert(trimmedOutput == expected)
      }
      trimmedOutput0
    }
  }
}

