package utest
package framework
import acyclic.file
import scala.util.{Failure, Success}

/**
 * Default implementation of [[Formatter]], also used by the default SBT test
 * framework. Allows some degree of customization of the formatted test results.
 */
trait Formatter {

  def formatColor: Boolean = true
  def formatTruncate: Int = 500
  def formatTrace: Boolean = true

  private[this] def prettyTruncate(r: Result, errorFormatter: Throwable => String, offset: Result => String = _ => ""): String = {

    val colorStr = if (r.value.isSuccess) Console.GREEN else Console.RED
    val cutUnit = r.value match{
      case Success(()) => "Success"
      case Success(v) => "Success " + ("\n"+v).replace("\n", "\n" + offset(r)  + Console.GREEN)
      case Failure(e) => errorFormatter(e)
    }

    val truncUnit =
      if (cutUnit.length <= formatTruncate) cutUnit
      else cutUnit.take(formatTruncate) + "..."

    if (formatColor) colorStr + truncUnit + Console.RESET else truncUnit
  }

  def formatSingle(path: Seq[String], r: Result): Option[String] = Some{
    path.map("." + _).mkString + "\t\t" + prettyTruncate(
      r,
      e => s"${("\n"+e.toString).replace("\n", "\n" + (" " * r.name.length) + "\t\t" + Console.RED)}"
    )
  }

  def format(results: Tree[Result]): Option[String] = Some{
    def errorFormatter(ex: Throwable): String =
      s"Failure('$ex'${Option(ex.getCause).fold("")(cause => s" caused by '$cause'")})"

    results.map(r =>
      r.name + "\t\t" + prettyTruncate(r, errorFormatter, r => (" " * r.name.length))
    ).reduce(_ + _.map("\n" + _).mkString.replace("\n", "\n    "))
  }
}

