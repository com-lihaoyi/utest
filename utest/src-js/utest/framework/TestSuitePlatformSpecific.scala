package utest.framework

trait TestSuitePlatformSpecific {

}

object TestSuitePlatformSpecific {
  def processGolden(allSuites: Seq[utest.TestSuite], goldenLiteralPrinter: Any => String): Unit = ()
}