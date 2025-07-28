package test.utest

import utest.*
import utest.framework.GoldenFix
import java.nio.file.Path
object GoldenFixTests extends utest.TestSuite {

  val tests = Tests {
    test("single") {
      val replaced = GoldenFix.applyToText(
        "0123456789",
        Seq(GoldenFix(Path.of("/tmp"), "Hello", 0, 0))
      )
      Predef.assert(replaced == "Hello0123456789")
    }
    test("middle") {
      val replaced = GoldenFix.applyToText(
        "0123456789",
        Seq(GoldenFix(Path.of("/tmp"), "Hello", 5, 5))
      )
      Predef.assert(replaced == "01234Hello56789")
    }
    test("replace") {
      val replaced = GoldenFix.applyToText(
        "0123456789",
        Seq(GoldenFix(Path.of("/tmp"), "Hello", 4, 6))
      )
      Predef.assert(replaced == "0123Hello6789")
    }
    test("replaceTwice") {
      val replaced = GoldenFix.applyToText(
        "0123456789",
        Seq(GoldenFix(Path.of("/tmp"), "Hello", 0, 1), GoldenFix(Path.of("/tmp"), "World", 5, 6))
      )
      utest.shaded.pprint.log(replaced)
      Predef.assert(replaced == "Hello1234World6789")
    }
    test("replaceIndented") {
      val replaced = GoldenFix.applyToText(
        """Hello
          |World
          |""".stripMargin,
        Seq(GoldenFix(Path.of("/tmp"), "I am\nCow", 2, 4))
      )
      utest.shaded.pprint.log(replaced)
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
          GoldenFix(Path.of("/tmp"), "I am\nCow", 2, 4),
          GoldenFix(Path.of("/tmp"), "Hear\nMe\nMoo", 7, 8)
        )
      )
      utest.shaded.pprint.log(replaced)
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



