package utest
package framework

//import acyclic.file

object DefaultFormatters{
  def formatSummary(resultsHeader: fansi.Str,
                    body: fansi.Str,
                    failureMsg: fansi.Str,
                    successCount: Int,
                    failureCount: Int,
                    showSummaryThreshold: Int): fansi.Str = {
    val totalCount = successCount + failureCount
    val summary: fansi.Str =
      if (totalCount < showSummaryThreshold) ""
      else fansi.Str.join(
        resultsHeader, "\n",
        body, "\n",
        failureMsg, "\n"
      )
    fansi.Str.join(
      summary,
      s"Tests: ", totalCount.toString, ", ",
      s"Passed: ", successCount.toString, ", ",
      s"Failed: ", failureCount.toString
    ).render
  }

  def resultsHeader = renderBanner("Results")
  def failureHeader = renderBanner("Failures")
  def renderBanner(s: String) = {
    val dashes = "-" * ((78 - s.length) / 2)
    dashes + " " + s + " " + dashes
  }
}