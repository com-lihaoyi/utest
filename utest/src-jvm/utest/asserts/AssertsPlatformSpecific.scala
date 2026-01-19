package utest.asserts

import utest.framework.{GoldenFix}
import utest.{AssertionError, TestValue}
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._
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

  /**
   * Asserts that the contents of the folder at [[actualFolderPath]] match the contents
   * of the golden folder at [[goldenFolderPath]]. Compares all files recursively.
   * If `UTEST_UPDATE_GOLDEN_TESTS=1` is set during the test run, the golden folder
   * is updated to match the actual folder contents.
   */
  def assertGoldenFolder(actualFolderPath: Path, goldenFolderPath: Path): Unit = {
    def listFilesRecursively(root: Path): Set[Path] = {
      if (!Files.exists(root)) Set.empty
      else {
        Files.walk(root).iterator().asScala
          .filter(Files.isRegularFile(_))
          .map(root.relativize)
          .toSet
      }
    }

    def readFileBytes(base: Path, relativePath: Path): Array[Byte] = {
      val fullPath = base.resolve(relativePath)
      if (Files.exists(fullPath)) Files.readAllBytes(fullPath) else Array.empty
    }

    def isBinaryContent(bytes: Array[Byte]): Boolean = {
      // Check for null bytes or high proportion of non-printable characters
      bytes.exists(_ == 0) || {
        val nonPrintable = bytes.count(b => b < 32 && b != '\t' && b != '\n' && b != '\r')
        bytes.nonEmpty && nonPrintable.toDouble / bytes.length > 0.1
      }
    }

    def formatFileContent(bytes: Array[Byte]): GoldenFix.Literal = {
      if (isBinaryContent(bytes)) {
        new GoldenFix.Literal(s"<binary file, ${bytes.length} bytes>")
      } else {
        new GoldenFix.Literal(new String(bytes, java.nio.charset.StandardCharsets.UTF_8))
      }
    }

    val goldenFiles = listFilesRecursively(goldenFolderPath)
    val actualFiles = listFilesRecursively(actualFolderPath)

    val onlyInGolden = goldenFiles -- actualFiles
    val onlyInActual = actualFiles -- goldenFiles
    val inBoth = goldenFiles.intersect(actualFiles)

    val differentContent = inBoth.filter { relativePath =>
      !java.util.Arrays.equals(
        readFileBytes(goldenFolderPath, relativePath),
        readFileBytes(actualFolderPath, relativePath)
      )
    }

    val hasDifferences = onlyInGolden.nonEmpty || onlyInActual.nonEmpty || differentContent.nonEmpty

    if (hasDifferences) {
      if (!sys.env.get("UTEST_UPDATE_GOLDEN_TESTS").exists(_.nonEmpty)) {
        // Build error message showing all differences
        val errorParts = Seq.newBuilder[TestValue]

        // Show files only in golden (will be deleted)
        for (relativePath <- onlyInGolden.toSeq.sortBy(_.toString)) {
          val goldenBytes = readFileBytes(goldenFolderPath, relativePath)
          errorParts += TestValue.Equality(
            TestValue.Single(s"$relativePath (golden)", None, formatFileContent(goldenBytes)),
            TestValue.Single(s"$relativePath (actual)", None, new GoldenFix.Literal("<file does not exist>"))
          )
        }

        // Show files only in actual (will be added)
        for (relativePath <- onlyInActual.toSeq.sortBy(_.toString)) {
          val actualBytes = readFileBytes(actualFolderPath, relativePath)
          errorParts += TestValue.Equality(
            TestValue.Single(s"$relativePath (golden)", None, new GoldenFix.Literal("<file does not exist>")),
            TestValue.Single(s"$relativePath (actual)", None, formatFileContent(actualBytes))
          )
        }

        // Show files with different content
        for (relativePath <- differentContent.toSeq.sortBy(_.toString)) {
          val goldenBytes = readFileBytes(goldenFolderPath, relativePath)
          val actualBytes = readFileBytes(actualFolderPath, relativePath)
          errorParts += TestValue.Equality(
            TestValue.Single(s"$relativePath (golden)", None, formatFileContent(goldenBytes)),
            TestValue.Single(s"$relativePath (actual)", None, formatFileContent(actualBytes))
          )
        }

        throw new AssertionError(
          s"Actual folder does not match golden folder\n" +
            s"golden: $goldenFolderPath\n" +
            s"actual: $actualFolderPath\n" +
            "Run tests with UTEST_UPDATE_GOLDEN_TESTS=1 to update the golden folder",
          errorParts.result()
        )
      } else {
        // Update mode: sync golden folder to match actual folder
        println(s"UTEST_UPDATE_GOLDEN_TESTS detected, syncing golden folder $goldenFolderPath with $actualFolderPath")

        // Delete files that only exist in golden
        for (relativePath <- onlyInGolden) {
          val targetPath = goldenFolderPath.resolve(relativePath)
          println(s"  Deleting $relativePath")
          Files.deleteIfExists(targetPath)
        }

        // Copy files that only exist in actual
        for (relativePath <- onlyInActual) {
          val sourcePath = actualFolderPath.resolve(relativePath)
          val targetPath = goldenFolderPath.resolve(relativePath)
          println(s"  Adding $relativePath")
          Files.createDirectories(targetPath.getParent)
          Files.copy(sourcePath, targetPath)
        }

        // Update files with different content
        for (relativePath <- differentContent) {
          val sourcePath = actualFolderPath.resolve(relativePath)
          val targetPath = goldenFolderPath.resolve(relativePath)
          println(s"  Updating $relativePath")
          Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }

        // Clean up empty directories in golden folder
        if (onlyInGolden.nonEmpty) {
          Files.walk(goldenFolderPath).iterator().asScala.toSeq
            .sortBy(_.toString)(Ordering[String].reverse) // Process deepest first
            .filter(Files.isDirectory(_))
            .filter(p => !Files.list(p).iterator().hasNext)
            .foreach(Files.delete)
        }
      }
    }
  }
}
