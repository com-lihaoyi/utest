package test.utest
import utest._
import concurrent.ExecutionContext.Implicits.global

/**
  * This only runs on the JVM test suite, since the stack traces tend to look
  * pretty different on Scala.js and Scala-native.
  */
object FormatterTests extends utest.TestSuite {
  def trim(s: String): String = {
    // Predef.augmentString = work around scala/bug#11125
    Predef.augmentString(s.trim).lines.map(_.reverse.dropWhile(_ == ' ').reverse)
      .mkString("\n")
      .replaceAll(" \\d+ms", "")
      .replaceAll(":\\d+\\)", ":\\)")
  }
  val tests = Tests{
    val tests = Tests {
      test("test1"){
        val x = 1
        try assert(x == 2)
        catch{case e: Throwable =>
          throw new Exception("wrapper", e)
        }
      }
      test("test2"){ 1 }

      test("test3"){
        val a = List[Byte](1, 2)
        a(10)
      }
    }

    test("simple"){
      val boa = new java.io.ByteArrayOutputStream()
      val printStream = new java.io.PrintStream(boa)

      val results = TestRunner.runAndPrint(
        tests,
        "MyTestSuite",
        printStream = printStream
      )

      utest.framework.Formatter.formatSummary("MyTestSuite", results).foreach(printStream.println)
      val trimmedOutput = trim(utest.ufansi.Str(new String(boa.toByteArray)).plainText)

      // This is very confusing to debug, with all the inner and outer test
      // traces being printed everywhere. Easier to paste it into the test `main`
      // method, running it there, and seeing why it looks wrong
      val expected = trim(
        """X MyTestSuite.test1
          |  java.lang.Exception: wrapper
          |    test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:)
          |    test.utest.FormatterTests$.$anonfun$tests$3(FormatterTests.scala:)
          |  utest.AssertionError: try assert(x == 2)
          |  x: Int = 1
          |    utest.asserts.AssertsCommons.assertImpl(AssertsCommons.scala:)
          |    utest.asserts.AssertsCommons.assertImpl$(AssertsCommons.scala:)
          |    utest.asserts.Asserts$.assertImpl(Asserts.scala:)
          |    test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:)
          |    test.utest.FormatterTests$.$anonfun$tests$3(FormatterTests.scala:)
          |+ MyTestSuite.test2  1
          |X MyTestSuite.test3
          |  java.lang.IndexOutOfBoundsException: 10
          |    scala.collection.LinearSeqOptimized.apply(LinearSeqOptimized.scala:)
          |    scala.collection.LinearSeqOptimized.apply$(LinearSeqOptimized.scala:)
          |    scala.collection.immutable.List.apply(List.scala:)
          |    test.utest.FormatterTests$.$anonfun$tests$6(FormatterTests.scala:)
          |- MyTestSuite
          |  X test1
          |    java.lang.Exception: wrapper
          |      test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:)
          |      test.utest.FormatterTests$.$anonfun$tests$3(FormatterTests.scala:)
          |    utest.AssertionError: try assert(x == 2)
          |    x: Int = 1
          |      utest.asserts.AssertsCommons.assertImpl(AssertsCommons.scala:)
          |      utest.asserts.AssertsCommons.assertImpl$(AssertsCommons.scala:)
          |      utest.asserts.Asserts$.assertImpl(Asserts.scala:)
          |      test.utest.FormatterTests$.liftedTree1$1(FormatterTests.scala:)
          |      test.utest.FormatterTests$.$anonfun$tests$3(FormatterTests.scala:)
          |  + test2  1
          |  X test3
          |    java.lang.IndexOutOfBoundsException: 10
          |      scala.collection.LinearSeqOptimized.apply(LinearSeqOptimized.scala:)
          |      scala.collection.LinearSeqOptimized.apply$(LinearSeqOptimized.scala:)
          |      scala.collection.immutable.List.apply(List.scala:)
          |      test.utest.FormatterTests$.$anonfun$tests$6(FormatterTests.scala:)
        """.stripMargin)

      assert(trimmedOutput == expected)
      trimmedOutput
    }

    test("wrapped"){
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

      // This is very confusing to debug, with all the inner and outer test
      // traces being printed everywhere. Easier to paste it into the test `main`
      // method, running it there, and seeing why it looks wrong
      val expected = trim(
        """X MyTestSuite.test1
          |  java.lang.Exception: wrapper
          |    test.utest.FormatterTests$.liftedTree1$1(Forma
          |    tterTests.scala:)
          |    test.utest.FormatterTests$.$anonfun$tests$3(Fo
          |    rmatterTests.scala:)
          |  utest.AssertionError: try assert(x == 2)
          |  x: Int = 1
          |    utest.asserts.AssertsCommons.assertImpl(Assert
          |    sCommons.scala:)
          |    utest.asserts.AssertsCommons.assertImpl$(Asser
          |    tsCommons.scala:)
          |    utest.asserts.Asserts$.assertImpl(Asserts.scal
          |    a:)
          |    test.utest.FormatterTests$.liftedTree1$1(Forma
          |    tterTests.scala:)
          |    test.utest.FormatterTests$.$anonfun$tests$3(Fo
          |    rmatterTests.scala:)
          |+ MyTestSuite.test2  1
          |X MyTestSuite.test3
          |  java.lang.IndexOutOfBoundsException: 10
          |    scala.collection.LinearSeqOptimized.apply(Line
          |    arSeqOptimized.scala:)
          |    scala.collection.LinearSeqOptimized.apply$(Lin
          |    earSeqOptimized.scala:)
          |    scala.collection.immutable.List.apply(List.sca
          |    la:)
          |    test.utest.FormatterTests$.$anonfun$tests$6(Fo
          |    rmatterTests.scala:)
          |- MyTestSuite
          |  X test1
          |    java.lang.Exception: wrapper
          |      test.utest.FormatterTests$.liftedTree1$1(For
          |      matterTests.scala:)
          |      test.utest.FormatterTests$.$anonfun$tests$3(
          |      FormatterTests.scala:)
          |    utest.AssertionError: try assert(x == 2)
          |    x: Int = 1
          |      utest.asserts.AssertsCommons.assertImpl(Asse
          |      rtsCommons.scala:)
          |      utest.asserts.AssertsCommons.assertImpl$(Ass
          |      ertsCommons.scala:)
          |      utest.asserts.Asserts$.assertImpl(Asserts.sc
          |      ala:)
          |      test.utest.FormatterTests$.liftedTree1$1(For
          |      matterTests.scala:)
          |      test.utest.FormatterTests$.$anonfun$tests$3(
          |      FormatterTests.scala:)
          |  + test2  1
          |  X test3
          |    java.lang.IndexOutOfBoundsException: 10
          |      scala.collection.LinearSeqOptimized.apply(Li
          |      nearSeqOptimized.scala:)
          |      scala.collection.LinearSeqOptimized.apply$(L
          |      inearSeqOptimized.scala:)
          |      scala.collection.immutable.List.apply(List.s
          |      cala:)
          |      test.utest.FormatterTests$.$anonfun$tests$6(
          |      FormatterTests.scala:)
        """.stripMargin)

      assert(trimmedOutput == expected)
      trimmedOutput
    }
  }
}
