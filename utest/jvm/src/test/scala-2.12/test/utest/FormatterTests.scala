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
  val tests = Tests{
    val tests = Tests {
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

    "simple" - {
      var trimmedOutput0: String = ""
      for(i <- 0 until 10) {
        val boa = new java.io.ByteArrayOutputStream()
        val printStream = new java.io.PrintStream(boa)

        val results = TestRunner.runAndPrint(
          tests,
          "MyTestSuite",
          printStream = printStream
        )

        utest.framework.Formatter.formatSummary("MyTestSuite", results).foreach(printStream.println)
        val trimmedOutput = trim(utest.ufansi.Str(new String(boa.toByteArray)).plainText)
        trimmedOutput0 = trimmedOutput
        // This is very confusing to debug, with all the inner and outer test
        // traces being printed everywhere. Easier to paste it into the test `main`
        // method, running it there, and seeing why it looks wrong
        val expected = trim(
          """X MyTestSuite.test1 0ms
            |  java.lang.Exception: wrapper
            |    test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:19)
            |    test.utest.FormatterTests$.$anonfun$tests$3(FormatterTests.scala:17)
            |  utest.AssertionError: try assert(x == 2)
            |  x: Int = 1
            |    utest.asserts.Asserts$.assertImpl(Asserts.scala:114)
            |    test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:17)
            |    test.utest.FormatterTests$.$anonfun$tests$3(FormatterTests.scala:17)
            |+ MyTestSuite.test2 0ms  1
            |X MyTestSuite.test3 0ms
            |  java.lang.IndexOutOfBoundsException: 10
            |    scala.collection.LinearSeqOptimized.apply(LinearSeqOptimized.scala:63)
            |    scala.collection.LinearSeqOptimized.apply$(LinearSeqOptimized.scala:61)
            |    scala.collection.immutable.List.apply(List.scala:86)
            |    test.utest.FormatterTests$.$anonfun$tests$6(FormatterTests.scala:26)
            |- MyTestSuite 0ms
            |  X test1 0ms
            |    java.lang.Exception: wrapper
            |      test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:19)
            |      test.utest.FormatterTests$.$anonfun$tests$3(FormatterTests.scala:17)
            |    utest.AssertionError: try assert(x == 2)
            |    x: Int = 1
            |      utest.asserts.Asserts$.assertImpl(Asserts.scala:114)
            |      test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:17)
            |      test.utest.FormatterTests$.$anonfun$tests$3(FormatterTests.scala:17)
            |  + test2 0ms  1
            |  X test3 0ms
            |    java.lang.IndexOutOfBoundsException: 10
            |      scala.collection.LinearSeqOptimized.apply(LinearSeqOptimized.scala:63)
            |      scala.collection.LinearSeqOptimized.apply$(LinearSeqOptimized.scala:61)
            |      scala.collection.immutable.List.apply(List.scala:86)
            |      test.utest.FormatterTests$.$anonfun$tests$6(FormatterTests.scala:26)
          """.stripMargin
        )

        // Warm up the code a few times first to ensure it finishes in 0ms per test
        if (i == 9) assert(trimmedOutput == expected)
      }
      trimmedOutput0
    }

    "wrapped" - {


      var trimmedOutput0: String = ""
      for(i <- 0 until 10) {
        val boa = new java.io.ByteArrayOutputStream()
        val printStream = new java.io.PrintStream(boa)
        val wrappingFormatter = new utest.framework.Formatter{
          override def formatWrapWidth = 50
        }
        val results = TestRunner.runAndPrint(
          tests,
          "MyTestSuite",
          formatter = wrappingFormatter,
          printStream = printStream
        )
        wrappingFormatter.formatSummary("MyTestSuite", results).foreach(printStream.println)
        val trimmedOutput = trim(utest.ufansi.Str(new String(boa.toByteArray)).plainText)
        trimmedOutput0 = trimmedOutput
        // This is very confusing to debug, with all the inner and outer test
        // traces being printed everywhere. Easier to paste it into the test `main`
        // method, running it there, and seeing why it looks wrong
        val expected = trim(
          """X MyTestSuite.test1 0ms
            |  java.lang.Exception: wrapper
            |    test.utest.FormatterTests$.liftedTree1$1(Forma
            |    tterTests.scala:19)
            |    test.utest.FormatterTests$.$anonfun$tests$3(Fo
            |    rmatterTests.scala:17)
            |  utest.AssertionError: try assert(x == 2)
            |  x: Int = 1
            |    utest.asserts.Asserts$.assertImpl(Asserts.scal
            |    a:114)
            |    test.utest.FormatterTests$.liftedTree1$1(Forma
            |    tterTests.scala:17)
            |    test.utest.FormatterTests$.$anonfun$tests$3(Fo
            |    rmatterTests.scala:17)
            |+ MyTestSuite.test2 0ms  1
            |X MyTestSuite.test3 0ms
            |  java.lang.IndexOutOfBoundsException: 10
            |    scala.collection.LinearSeqOptimized.apply(Line
            |    arSeqOptimized.scala:63)
            |    scala.collection.LinearSeqOptimized.apply$(Lin
            |    earSeqOptimized.scala:61)
            |    scala.collection.immutable.List.apply(List.sca
            |    la:86)
            |    test.utest.FormatterTests$.$anonfun$tests$6(Fo
            |    rmatterTests.scala:26)
            |- MyTestSuite 0ms
            |  X test1 0ms
            |    java.lang.Exception: wrapper
            |      test.utest.FormatterTests$.liftedTree1$1(For
            |      matterTests.scala:19)
            |      test.utest.FormatterTests$.$anonfun$tests$3(
            |      FormatterTests.scala:17)
            |    utest.AssertionError: try assert(x == 2)
            |    x: Int = 1
            |      utest.asserts.Asserts$.assertImpl(Asserts.sc
            |      ala:114)
            |      test.utest.FormatterTests$.liftedTree1$1(For
            |      matterTests.scala:17)
            |      test.utest.FormatterTests$.$anonfun$tests$3(
            |      FormatterTests.scala:17)
            |  + test2 0ms  1
            |  X test3 0ms
            |    java.lang.IndexOutOfBoundsException: 10
            |      scala.collection.LinearSeqOptimized.apply(Li
            |      nearSeqOptimized.scala:63)
            |      scala.collection.LinearSeqOptimized.apply$(L
            |      inearSeqOptimized.scala:61)
            |      scala.collection.immutable.List.apply(List.s
            |      cala:86)
            |      test.utest.FormatterTests$.$anonfun$tests$6(
            |      FormatterTests.scala:26)
          """.stripMargin
        )

        // Warm up the code a few times first to ensure it finishes in 0ms per test
        if (i == 9) assert(trimmedOutput == expected)
      }
      trimmedOutput0
    }
  }
}
