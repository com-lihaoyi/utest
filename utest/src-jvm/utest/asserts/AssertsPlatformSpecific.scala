package utest.asserts

import utest.framework.{GoldenFix}
import utest.{AssertionError, TestValue}
import java.nio.file.{Files, Path}
trait AssertsPlatformSpecific {
  def assertGoldenFile(testValue: String, path: Path)(implicit reporter: GoldenFix.Reporter): Unit = {
    val goldenFileContents = if (Files.exists(path)) Files.readString(path) else ""
    if (goldenFileContents != testValue) {
      if (!sys.env.contains("UTEST_UPDATE_GOLDEN_TESTS")) {
        throw new AssertionError(
          "Value does not match golden file contents\n" +
          "Run UTEST_UPDATE_GOLDEN_TESTS=1 to update golden file " + path,
          Seq(
            TestValue.Equality(
              TestValue.Single("goldenFileContents", None, goldenFileContents),
              TestValue.Single("testValue", None, testValue),
            )
          )
        )
      }
      else {
        reporter.apply(GoldenFix(path, testValue, 0, goldenFileContents.length))
      }
    }
  }

  def assertGoldenLiteral(testValue: Any, golden: GoldenFix.Span[Any])
                         (implicit reporter: GoldenFix.Reporter): Unit = {
    val goldenValue = golden.value
    if (testValue != goldenValue) {
      if (!sys.env.contains("UTEST_UPDATE_GOLDEN_TESTS")) {
        throw new AssertionError(
          "Value does not match golden literal contents\n" +
            "Run UTEST_UPDATE_GOLDEN_TESTS=1 to update golden literal in " + golden.sourceFile,
          Seq(
            TestValue.Equality(
              TestValue.Single("testValue", None, testValue),
              TestValue.Single("goldenValue", None, goldenValue),
            )
          )
        )
      } else {
        reporter.apply(
          GoldenFix(
            Path.of(golden.sourceFile),
            utest.shaded.pprint.PPrinter.BlackWhite.apply(golden.value).plainText,
            golden.startOffset,
            golden.endOffset
          )
        )
      }
    }
  }
}
