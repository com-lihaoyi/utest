package utest
package framework

//import acyclic.file

object DefaultFormatters{
  def formatSummary(resultsHeader: utest.shaded.fansi.Str,
                    body: utest.shaded.fansi.Str,
                    failureMsg: utest.shaded.fansi.Str,
                    successCount: Int,
                    failureCount: Int,
                    showSummaryThreshold: Int): utest.shaded.fansi.Str = {
    val totalCount = successCount + failureCount
    val summary: utest.shaded.fansi.Str =
      if (totalCount < showSummaryThreshold) ""
      else utest.shaded.fansi.Str.join(Seq(
        resultsHeader, "\n",
        body, "\n",
        failureMsg, "\n"
      ))
    utest.shaded.fansi.Str.join(Seq(
      summary,
      s"Tests: ", totalCount.toString, ", ",
      s"Passed: ", successCount.toString, ", ",
      s"Failed: ", failureCount.toString
    )).render
  }

  def resultsHeader = renderBanner("Results")
  def failureHeader = renderBanner("Failures")
  def renderBanner(s: String) = {
    val dashes = "-" * ((78 - s.length) / 2)
    dashes + " " + s + " " + dashes
  }
}