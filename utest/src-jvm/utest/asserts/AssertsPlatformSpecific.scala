package utest.asserts

import utest.framework.{GoldenFix}
import utest.{AssertionError, TestValue}

trait AssertsPlatformSpecific {
  def assertGoldenFile(path: java.nio.file.Path, testValue: String)(implicit reporter: GoldenFix.Reporter): Unit = {
    val goldenFileContents = java.nio.file.Files.readString(path)
    if (goldenFileContents != testValue) {
      if (!sys.env.contains("UTEST_UPDATE_GOLDEN_TESTS")) {
        throw new AssertionError(
          "Value does not match golden file contents: " + path,
          Seq(
            TestValue.Equality(
              TestValue.Single("goldenFileContents", None, goldenFileContents),
              TestValue.Single("testValue", None, testValue),
            )
          )
        )

      }
      else {
        System.err.println("Value does not match golden file contents: " + path)
        reporter.apply(GoldenFix(path, goldenFileContents, 0, goldenFileContents.length))
      }
    }
  }

  def assertGoldenLiteral(testValue: Any, golden: GoldenFix.Span[Any])
                         (implicit reporter: GoldenFix.Reporter): Unit = {
    val goldenValue = golden.value
    if (testValue != goldenValue) {
      if (!sys.env.contains("UTEST_UPDATE_GOLDEN_TESTS")) {
        throw new AssertionError(
          "Value does not match golden literal contents",
          Seq(
            TestValue.Equality(
              TestValue.Single("testValue", None, testValue),
              TestValue.Single("goldenValue", None, goldenValue),
            )
          )
        )
      } else {
        System.err.println("Value does not match golden literal contents in: " + golden.sourceFile)
        reporter.apply(
          GoldenFix(
            java.nio.file.Path.of(golden.sourceFile),
            utest.shaded.pprint.PPrinter.BlackWhite.apply(golden.value).plainText,
            golden.startOffset,
            golden.endOffset
          )
        )
      }
    }
  }
}
