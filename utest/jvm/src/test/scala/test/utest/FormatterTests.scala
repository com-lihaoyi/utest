package test.utest
import utest._
import concurrent.ExecutionContext.Implicits.global

/**
  * This only runs on the JVM test suite, since the stack traces tend to look
  * pretty different on Scala.js and Scala-native.
  */
object FormatterTests extends utest.TestSuite {
  def trim(s: String): String = {
    s.trim.lines.map(_.reverse.dropWhile(_ == ' ').reverse).mkString("\n")
  }
  val tests = this{
    "hello"-{
      var trimmedOutput0: String = ""
      for(i <- 0 until 10) {
        val boa = new java.io.ByteArrayOutputStream()
        val printStream = new java.io.PrintStream(boa)
        val tests = TestSuite {
          'test1 - {
            val x = 1
            try assert(x == 2)
            catch{case e: Throwable =>
              throw new Exception("wrapper", e)
            }
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
        val trimmedOutput = trim(utest.fansi.Str(new String(boa.toByteArray)).plainText)
        trimmedOutput0 = trimmedOutput
        val expected = trim(
          """X MyTestSuite.test1 0ms
            |  java.lang.Exception: wrapper
            |  test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:24)
            |  test.utest.FormatterTests$.$anonfun$tests$9(FormatterTests.scala:22)
            |  utest.AssertionError: try assert(x == 2)
            |  x: Int = 1
            |  test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:22)
            |  test.utest.FormatterTests$.$anonfun$tests$9(FormatterTests.scala:22)
            |+ MyTestSuite.test2 0ms  1
            |X MyTestSuite.test3 0ms
            |  java.lang.IndexOutOfBoundsException: 10
            |  scala.collection.LinearSeqOptimized.apply(LinearSeqOptimized.scala:63)
            |  scala.collection.LinearSeqOptimized.apply$(LinearSeqOptimized.scala:61)
            |  scala.collection.immutable.List.apply(List.scala:86)
            |  test.utest.FormatterTests$.$anonfun$tests$12(FormatterTests.scala:31)
            |- MyTestSuite
            |  X test1 0ms
            |    java.lang.Exception: wrapper
            |    test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:24)
            |    test.utest.FormatterTests$.$anonfun$tests$9(FormatterTests.scala:22)
            |    utest.AssertionError: try assert(x == 2)
            |    x: Int = 1
            |    test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:22)
            |    test.utest.FormatterTests$.$anonfun$tests$9(FormatterTests.scala:22)
            |  + test2 0ms  1
            |  X test3 0ms
            |    java.lang.IndexOutOfBoundsException: 10
            |    scala.collection.LinearSeqOptimized.apply(LinearSeqOptimized.scala:63)
            |    scala.collection.LinearSeqOptimized.apply$(LinearSeqOptimized.scala:61)
            |    scala.collection.immutable.List.apply(List.scala:86)
            |    test.utest.FormatterTests$.$anonfun$tests$12(FormatterTests.scala:31)
          """.stripMargin
        )

        // Warm up the code a few times first to ensure it finishes in 0ms per test
        if (i == 9) assert(trimmedOutput == expected)
      }
      trimmedOutput0
    }
  }
}
