package utest
package framework

//import acyclic.file

object DefaultFormatters{
  def formatSummary(resultsHeader: ufansi.Str,
                    body: ufansi.Str,
                    failureMsg: ufansi.Str,
                    successCount: Int,
                    failureCount: Int,
                    showSummaryThreshold: Int): ufansi.Str = {
    val totalCount = successCount + failureCount
    val summary: ufansi.Str =
      if (totalCount < showSummaryThreshold) ""
      else ufansi.Str.join(
        resultsHeader, "\n",
        body, "\n",
        failureMsg, "\n"
      )
    ufansi.Str.join(
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