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
  def formatTruncate: Int = 5000
  def formatTrace: Boolean = true

  def formatValueColor: fansi.Attrs =
    if (formatColor) fansi.Color.Blue
    else fansi.Attrs.Empty

  def formatResultColor(success: Boolean): fansi.Attrs =
    if (!formatColor) fansi.Attrs.Empty
    else if (success) fansi.Color.Green
    else fansi.Color.Red

  def formatMillisColor: fansi.Attrs =
    if (formatColor) fansi.Color.DarkGray
    else fansi.Attrs.Empty

  private[this] def prettyTruncate(r: Result,
                                   errorFormatter: Throwable => fansi.Str,
                                   offset: Result => String = _ => ""): fansi.Str = {


    val cutUnit: fansi.Str = r.value match{
      case Success(()) => ""
      case Success(v) => formatValueColor(v.toString.replace("\n", "\n" + offset(r)))
      case Failure(e) => errorFormatter(e)
    }

    val truncUnit =
      if (cutUnit.length <= formatTruncate) cutUnit
      else cutUnit.substring(0, formatTruncate) ++ "..."

    truncUnit
  }

  def wrapLabel(r: Result, label: String) = {
    fansi.Str.join(
      formatIcon(r.value.isInstanceOf[Success[_]]), " ",
      label, " ",
      formatMillisColor(r.milliDuration + "ms"), " ",
      prettyTruncate(
        r,
        e => s"${("\n"+e.toString).replace("\n", "\n" + (" " * r.name.length) + "\t\t" + formatResultColor(false))}"
      )
    )
  }

  def formatSingle(path: Seq[String], r: Result): Option[fansi.Str] = Some{
    wrapLabel(r, path.mkString("."))
  }

  def formatIcon(success: Boolean): fansi.Str = {
    formatResultColor(success)(if (success) "+" else "X")
  }
  def format(results: Tree[Result]): Option[fansi.Str] = Some{
    def errorFormatter(ex: Throwable): String =
      s"Failure('$ex'${Option(ex.getCause).fold("")(cause => s" caused by '$cause'")})"

    results
      .map { r => wrapLabel(r, r.name) }
      .reduce(_ + _.map("\n" + _)
      .mkString
      .replace("\n", "\n  "))
  }
}

