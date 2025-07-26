package utest
package framework

//import acyclic.file

object DefaultFormatters{
  def formatSummary(resultsHeader: utest.fansi.Str,
                    body: utest.fansi.Str,
                    failureMsg: utest.fansi.Str,
                    successCount: Int,
                    failureCount: Int,
                    showSummaryThreshold: Int): utest.fansi.Str = {
    val totalCount = successCount + failureCount
    val summary: utest.fansi.Str =
      if (totalCount < showSummaryThreshold) ""
      else utest.fansi.Str.join(Seq(
        resultsHeader, "\n",
        body, "\n",
        failureMsg, "\n"
      ))
    utest.fansi.Str.join(Seq(
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