package utest.asserts

import utest.framework.{GoldenFix}
import utest.{AssertionError, TestValue}
import java.nio.file.{Files, Path}
trait AssertsPlatformSpecific {
  private def throwAssertionError(path: String, goldenValue: Any, actualValue: Any): Unit = {
    throw new AssertionError(
      s"Actual value does not match golden data in file\n$path\n" +
        "Run tests with UTEST_UPDATE_GOLDEN_TESTS=1 to apply the following patch to update the golden value",
      Seq(
        TestValue.Equality(
          TestValue.Single("goldenValue", None, goldenValue),
          TestValue.Single("actualValue", None, actualValue),
        )
      )
    )
  }

  /**
   * Asserts that the `String` [[actualValue]] is equivalent to the contents of
   * the file on disk at [[goldenFilePath]]. If `UTEST_UPDATE_GOLDEN_TESTS=1` is
   * set during the test run, the golden file is updated to the latest contents of [[actualValue]]
   */
  def assertGoldenFile(actualValue: String, goldenFilePath: Path)(implicit reporter: GoldenFix.Reporter): Unit = {
    val goldenFileContents = if (Files.exists(goldenFilePath)) Files.readString(goldenFilePath) else ""
    if (goldenFileContents != actualValue) {
      if (!sys.env.get("UTEST_UPDATE_GOLDEN_TESTS").exists(_.nonEmpty)) {
        throwAssertionError(goldenFilePath.toString, new GoldenFix.Literal(goldenFileContents), new GoldenFix.Literal(actualValue))
      } else {
        reporter.apply(GoldenFix(goldenFilePath, new GoldenFix.Literal(actualValue), 0, goldenFileContents.length))
      }
    }
  }

  /**
   * Asserts that the `String` [[actualValue]] is equivalent to the contents of
   * the [[goldenLiteral]]. If `UTEST_UPDATE_GOLDEN_TESTS=1` is
   * set during the test run, the source code of [[goldenLiteral]] is updated to the latest
   * contents of [[actualValue]] pretty-printed using PPrint.
   */
  def assertGoldenLiteral(actualValue: Any, goldenLiteral: GoldenFix.Span[Any])
                         (implicit reporter: GoldenFix.Reporter): Unit = {
    Predef.assert(
      goldenLiteral != null,
      "assertGoldenLiteral does not allow `null` as the golden literal," +
        "please use `()` if you want a placeholder for `UTEST_UPDATE_GOLDEN_TESTS=1` to fill in"
    )
    val goldenValue = goldenLiteral.value
    if (actualValue != goldenValue) {
      if (!sys.env.get("UTEST_UPDATE_GOLDEN_TESTS").exists(_.nonEmpty)) {
        throwAssertionError(goldenLiteral.sourceFile, goldenValue, actualValue)
      } else {
        reporter.apply(
          GoldenFix(
            Path.of(goldenLiteral.sourceFile),
            actualValue,
            goldenLiteral.startOffset,
            goldenLiteral.endOffset
          )
        )
      }
    }
  }
}
