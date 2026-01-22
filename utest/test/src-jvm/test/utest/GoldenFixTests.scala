package test.utest

import utest._
import utest.framework.GoldenFix
import java.nio.file.{Files, Path}
object GoldenFixTests extends utest.TestSuite {

  val tests = Tests {
    test("assertGoldenFolder") {
      def deleteRecursively(path: Path): Unit = {
        if (Files.isDirectory(path)) {
          val stream = Files.list(path)
          try stream.forEach(deleteRecursively)
          finally stream.close()
        }
        Files.deleteIfExists(path)
      }

      test("mismatchedFolders") {
        val goldenDir = Files.createTempDirectory("golden")
        val actualDir = Files.createTempDirectory("actual")

        try {
          // Create a file only in golden (will show as deleted)
          Files.writeString(goldenDir.resolve("only-in-golden.txt"), "golden content")

          // Create a file only in actual (will show as added)
          Files.writeString(actualDir.resolve("only-in-actual.txt"), "actual content")

          // Create a file in both with different content
          Files.writeString(goldenDir.resolve("different.txt"), "old value")
          Files.writeString(actualDir.resolve("different.txt"), "new value")

          try {
            assertGoldenFolder(actualDir, goldenDir)
            Predef.assert(false, "Should have thrown AssertionError")
          } catch {
            case e: utest.AssertionError =>
              val msgLines = e.getMessage.linesIterator.toSeq
              // Filter out the temp directory paths which vary per run
              val filteredLines = msgLines.map { line =>
                if (line.startsWith("golden:") || line.startsWith("actual:")) line.split(":").head + ": <temp>"
                else line
              }
              // Order is: onlyInGolden (sorted), onlyInActual (sorted), differentContent (sorted)
              val expected = Seq(
                "Actual folder does not match golden folder",
                "golden: <temp>",
                "actual: <temp>",
                "Run tests with UTEST_UPDATE_GOLDEN_TESTS=1 to update the golden folder",
                "only-in-golden.txt (golden) != only-in-golden.txt (actual):",
                "- golden content",
                "+ <file does not exist>",
                "",
                "only-in-actual.txt (golden) != only-in-actual.txt (actual):",
                "- <file does not exist>",
                "+ actual content",
                "",
                "different.txt (golden) != different.txt (actual):",
                "- old value",
                "+ new value"
              )
              Predef.assert(
                filteredLines == expected,
                s"Error message mismatch.\nExpected:\n${expected.mkString("\n")}\n\nActual:\n${filteredLines.mkString("\n")}"
              )
          }
        } finally {
          deleteRecursively(goldenDir)
          deleteRecursively(actualDir)
        }
      }

      test("binaryFiles") {
        val goldenDir = Files.createTempDirectory("golden-binary")
        val actualDir = Files.createTempDirectory("actual-binary")

        try {
          // Create binary files with different content (containing null bytes)
          // PNG magic bytes followed by null bytes
          val goldenBinary = Array(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00).map(_.toByte)
          val actualBinary = Array(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0xFF, 0xFF).map(_.toByte)

          Files.write(goldenDir.resolve("image.png"), goldenBinary)
          Files.write(actualDir.resolve("image.png"), actualBinary)

          // Also add a binary file only in actual
          Files.write(actualDir.resolve("new-binary.dat"), Array(0x00, 0x01, 0x02, 0x03).map(_.toByte))

          try {
            assertGoldenFolder(actualDir, goldenDir)
            Predef.assert(false, "Should have thrown AssertionError")
          } catch {
            case e: utest.AssertionError =>
              val msgLines = e.getMessage.linesIterator.toSeq
              val filteredLines = msgLines.map { line =>
                if (line.startsWith("golden:") || line.startsWith("actual:")) line.split(":").head + ": <temp>"
                else line
              }
              // Order: onlyInActual first, then differentContent
              // For image.png both sides show "<binary file, 10 bytes>" so diff shows as unchanged
              val expected = Seq(
                "Actual folder does not match golden folder",
                "golden: <temp>",
                "actual: <temp>",
                "Run tests with UTEST_UPDATE_GOLDEN_TESTS=1 to update the golden folder",
                "new-binary.dat (golden) != new-binary.dat (actual):",
                "- <file does not exist>",
                "+ <binary file, 4 bytes>",
                "",
                "image.png (golden) != image.png (actual):",
                "  <binary file, 10 bytes>"
              )
              Predef.assert(
                filteredLines == expected,
                s"Error message mismatch.\nExpected:\n${expected.mkString("\n")}\n\nActual:\n${filteredLines.mkString("\n")}"
              )
          }
        } finally {
          deleteRecursively(goldenDir)
          deleteRecursively(actualDir)
        }
      }

      test("multiLineTextDiff") {
        val goldenDir = Files.createTempDirectory("golden-multiline")
        val actualDir = Files.createTempDirectory("actual-multiline")

        try {
          // Create multi-line files with one line changed
          val goldenText =
            """line 1
              |line 2
              |line 3
              |line 4
              |line 5""".stripMargin
          val actualText =
            """line 1
              |line 2
              |line THREE
              |line 4
              |line 5""".stripMargin

          Files.writeString(goldenDir.resolve("config.txt"), goldenText)
          Files.writeString(actualDir.resolve("config.txt"), actualText)

          try {
            assertGoldenFolder(actualDir, goldenDir)
            Predef.assert(false, "Should have thrown AssertionError")
          } catch {
            case e: utest.AssertionError =>
              val msgLines = e.getMessage.linesIterator.toSeq
              val filteredLines = msgLines.map { line =>
                if (line.startsWith("golden:") || line.startsWith("actual:")) line.split(":").head + ": <temp>"
                else line
              }
              val expected = Seq(
                "Actual folder does not match golden folder",
                "golden: <temp>",
                "actual: <temp>",
                "Run tests with UTEST_UPDATE_GOLDEN_TESTS=1 to update the golden folder",
                "config.txt (golden) != config.txt (actual):",
                "  line 1",
                "  line 2",
                "- line 3",
                "+ line THREE",
                "  line 4",
                "  line 5"
              )
              Predef.assert(
                filteredLines == expected,
                s"Error message mismatch.\nExpected:\n${expected.mkString("\n")}\n\nActual:\n${filteredLines.mkString("\n")}"
              )
          }
        } finally {
          deleteRecursively(goldenDir)
          deleteRecursively(actualDir)
        }
      }
    }
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



