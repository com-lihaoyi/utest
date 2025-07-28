package utest.framework

trait TestSuitePlatformSpecific {
  private[utest] val utestGoldenReports = collection.mutable.Buffer.empty[GoldenFix]
  protected implicit def utestGoldenReporter: GoldenFix.Reporter = fix => utestGoldenReports.synchronized{
    utestGoldenReports.append(fix)
  }
}

object TestSuitePlatformSpecific {
  def processGolden(allSuites: Seq[utest.TestSuite]): Unit = {
    println("TestSuitePlatformSpecific.processGolden: " + allSuites.length)
    if (sys.env.contains("UTEST_UPDATE_GOLDEN_TESTS")) {
      val goldenFixes = allSuites.flatMap { suite =>
        suite.utestGoldenReports.synchronized {
          suite.utestGoldenReports.toList
        }
      }
      GoldenFix.applyAll(goldenFixes)
    }
  }
}
