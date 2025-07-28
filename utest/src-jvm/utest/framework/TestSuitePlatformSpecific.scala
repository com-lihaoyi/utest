package utest.framework

trait TestSuitePlatformSpecific {
  private[utest] val utestGoldenReports = collection.mutable.Buffer.empty[GoldenFix]
  protected implicit def utestGoldenReporter: GoldenFix.Reporter = fix => utestGoldenReports.synchronized{
    utestGoldenReports.append(fix)
  }
}
