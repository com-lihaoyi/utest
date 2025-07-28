package test.utest

import utest._
import utest.framework.GoldenFix
import java.nio.file.Path
object GoldenFixTests extends utest.TestSuite {

  val tests = Tests {
    test("capture") {
      val x: GoldenFix.Span[Seq[Int]] = List(
        1,
        2,
        3
      )

      Predef.assert(
        java.nio.file.Files.readString(java.nio.file.Path.of(x.sourceFile)).slice(x.startOffset, x.endOffset) ==
        """List(
          |        1,
          |        2,
          |        3
          |      )""".stripMargin
      )
    }
    test("single") {
      val replaced = GoldenFix.applyToText(
        "0123456789",
        Seq(GoldenFix(null, new GoldenFix.Literal("Hello"), 0, 0)),
        utest.shaded.pprint.PPrinter.BlackWhite.apply(_).plainText
      )
      Predef.assert(replaced == "Hello0123456789")
    }
    test("middle") {
      val replaced = GoldenFix.applyToText(
        "0123456789",
        Seq(GoldenFix(null, new GoldenFix.Literal("Hello"), 5, 5)),
        utest.shaded.pprint.PPrinter.BlackWhite.apply(_).plainText
      )
      Predef.assert(replaced == "01234Hello56789")
    }
    test("replace") {
      val replaced = GoldenFix.applyToText(
        "0123456789",
        Seq(GoldenFix(null, new GoldenFix.Literal("Hello"), 4, 6)),
        utest.shaded.pprint.PPrinter.BlackWhite.apply(_).plainText
      )
      Predef.assert(replaced == "0123Hello6789")
    }
    test("replaceTwice") {
      val replaced = GoldenFix.applyToText(
        "0123456789",
        Seq(
          GoldenFix(null, new GoldenFix.Literal("Hello"), 0, 1),
          GoldenFix(null, new GoldenFix.Literal("World"), 5, 6)
        ),
        utest.shaded.pprint.PPrinter.BlackWhite.apply(_).plainText
      )
      Predef.assert(replaced == "Hello1234World6789")
    }
    test("replaceIndented") {
      val replaced = GoldenFix.applyToText(
        """Hello
          |World
          |""".stripMargin,
        Seq(GoldenFix(null, new GoldenFix.Literal("I am\nCow"), 2, 4)),
        utest.shaded.pprint.PPrinter.BlackWhite.apply(_).plainText
      )
      Predef.assert(
        replaced ==
          """HeI am
            |  Cowo
            |World
            |""".stripMargin
      )
    }
    test("replaceIndentedTwice") {
      val replaced = GoldenFix.applyToText(
        """Hello
          |World
          |""".stripMargin,
        Seq(
          GoldenFix(null, new GoldenFix.Literal("I am\nCow"), 2, 4),
          GoldenFix(null, new GoldenFix.Literal("Hear\nMe\nMoo"), 7, 8)
        ),
        utest.shaded.pprint.PPrinter.BlackWhite.apply(_).plainText
      )
      Predef.assert(
        replaced ==
          """HeI am
            |  Cowo
            |WHear
            | Me
            | Moorld
            |""".stripMargin
      )
    }
  }
}



