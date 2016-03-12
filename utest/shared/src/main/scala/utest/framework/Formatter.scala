package utest
package framework
//import acyclic.file
import scala.util.{Failure, Success}

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


    val cutUnit = r.value match{
      case Success(()) => "Success"
      case Success(v) =>
        "Success " + ("\n"+v).replace("\n", "\n" + offset(r)  + formatStartColor(true))
      case Failure(e) => errorFormatter(e)
    }

    val truncUnit =
      if (cutUnit.length <= formatTruncate) cutUnit
      else cutUnit.take(formatTruncate) + "..."

    if (formatColor) formatStartColor(r.value.isSuccess) + truncUnit + formatEndColor else truncUnit
  }

  def formatSingle(path: Seq[String], r: Result): Option[String] = Some{
    path.map("." + _).mkString + "\t\t" + prettyTruncate(
      r,
      e => s"${("\n"+e.toString).replace("\n", "\n" + (" " * r.name.length) + "\t\t" + formatStartColor(false))}"
    )
  }

  def format(results: Tree[Result]): Option[String] = Some{
    def errorFormatter(ex: Throwable): String =
      s"Failure('$ex'${Option(ex.getCause).fold("")(cause => s" caused by '$cause'")})"

    results.map(r =>
      r.name + "\t\t" + prettyTruncate(r, errorFormatter, r => " " * r.name.length)
    ).reduce(_ + _.map("\n" + _).mkString.replace("\n", "\n    "))
  }
}

