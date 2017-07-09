package utest
package framework
//import acyclic.file
import scala.util.{Failure, Success}
import utest.ColorStrings

/**
 * Default implementation of [[Formatter]], also used by the default SBT test
 * framework. Allows some degree of customization of the formatted test results.
 */
trait Formatter {

  def formatColor: Boolean = true
  def formatTruncate: Int = 500
  def formatTrace: Boolean = true

  def formatStartColor(success: Boolean) =
    if (!formatColor) ""
    else if (success) Console.GREEN
    else Console.RED

  def formatEndColor = if (formatColor) Console.RESET else ""
  private[this] def prettyTruncate(r: Result,
                                   errorFormatter: Throwable => String,
                                   offset: Result => String = _ => ""): String = {


    val durationStr = Console.RESET + (r.milliDuration.toString + " ms").faint
    val cutUnit = r.value match {
      case Success(()) => durationStr
      case Success(v: Any) =>
        val offsetStr = offset(r)
        durationStr + " \n" + offsetStr + v.toString.blue
      case Failure(e) => "\n" + errorFormatter(e)
    }

    val truncUnit =
      if (cutUnit.length <= formatTruncate) cutUnit
      else cutUnit.take(formatTruncate) + "..."

    if (formatColor) formatStartColor(success = r.value.isSuccess) + truncUnit + formatEndColor else truncUnit
  }

  private val successIcon = "\u25C9".green
  private val failureIcon = "\u25C9".red
  def format(results: Tree[Result]): Option[String] = Some {
    def errorFormatter(ex: Throwable): String = {
      val causation = Option(ex.getCause) match {
        case Some(cause) =>
          " caused by: " + cause.toString.red
        case None =>
           ""
      }

      val errStr = ex.toString
      val assertPrefix = "utest.AssertionError:"
      if (errStr.startsWith(assertPrefix)) {
        assertPrefix.bold.white.redBg + errStr.stripPrefix(assertPrefix)
      } else {
        errStr.bold.white.redBg + "\n" + causation
      }
    }

    results.map(result => {
      val isFailure = result.value.isFailure
      val icon = if (isFailure) failureIcon else successIcon
      val nameSegment = " " + result.name + " "
      val testNameText = if (isFailure) nameSegment.bold.white.redBg else nameSegment.bold
      icon + testNameText + prettyTruncate(result, errorFormatter, r => "  ")
    }
    ).reduce(_ + _.map("\n" + _).mkString.replace("\n", "\n  "))
  }
}
