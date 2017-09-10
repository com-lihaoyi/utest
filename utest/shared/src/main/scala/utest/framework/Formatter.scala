package utest
package framework
//import acyclic.file
import scala.collection.mutable
import scala.util.{Failure, Success}

object Formatter extends Formatter
/**
 * Default implementation of [[Formatter]], also used by the default SBT test
 * framework. Allows some degree of customization of the formatted test results.
 */
trait Formatter {

  def formatColor: Boolean = true
  def formatTruncate: Int = 200
  def formatTrace: Boolean = true
  def formatWrapThreshold: Int = 90

  def formatValue(x: Any) = formatValueColor(x.toString)

  def formatValueColor: fansi.Attrs =
    if (formatColor) fansi.Color.Blue
    else fansi.Attrs.Empty

  def formatException(x: Throwable) = {
    formatResultColor(false)(x.toString)
  }
  def formatResultColor(success: Boolean): fansi.Attrs =
    if (!formatColor) fansi.Attrs.Empty
    else if (success) fansi.Color.Green
    else fansi.Color.Red

  def formatMillisColor: fansi.Attrs =
    if (formatColor) fansi.Color.DarkGray
    else fansi.Attrs.Empty

  def indentMultilineStr(input: fansi.Str, leftIndent: String): fansi.Str = {
    fansi.Str.join(input.split('\n').flatMap(Seq[fansi.Str]("\n", leftIndent, _)).drop(2):_*)
  }
  private[this] def prettyTruncate(r: Result, leftIndent: String): fansi.Str = {


    val cutUnit: fansi.Str = r.value match{
      case Success(()) => ""
      case Success(v) => indentMultilineStr(formatValue(v), leftIndent)
      case Failure(e) => indentMultilineStr(formatException(e), leftIndent)
    }

    val truncUnit =
      if (cutUnit.length <= formatTruncate) cutUnit
      else cutUnit.substring(0, formatTruncate) ++ "..."

    truncUnit
  }

  def wrapLabel(leftIndentCount: Int, r: Result, label: String): fansi.Str = {
    val leftIndent = "  " * leftIndentCount
    val lhs = fansi.Str.join(
      leftIndent,
      formatIcon(r.value.isInstanceOf[Success[_]]), " ",
      label, " ",
      formatMillisColor(r.milliDuration + "ms"), " "
    )

    val rhs = prettyTruncate(r, leftIndent + "  ")


    val sep =
      if (lhs.length + rhs.length <= formatWrapThreshold) " "
      else "\n" + leftIndent + "  "

    lhs ++ sep ++ rhs
  }

  def formatSingle(path: Seq[String], r: Result): Option[fansi.Str] = Some{
    wrapLabel(0, r, path.mkString("."))
  }

  def formatIcon(success: Boolean): fansi.Str = {
    formatResultColor(success)(if (success) "+" else "X")
  }

  def format(topLevelName: String, results: HTree[String, Result]): Option[fansi.Str] = Some{
    val linearized = mutable.Buffer.empty[fansi.Str]

    val relabelled = results match{
      case HTree.Node(v, c@_*) => HTree.Node(topLevelName, c:_*)
      case HTree.Leaf(r) => HTree.Leaf(r.copy(name = topLevelName))
    }
    rec(0, relabelled){
      case (depth, Left(name)) => linearized.append("  " * depth + "- " + name)
      case (depth, Right(r)) => linearized.append(wrapLabel(depth, r, r.name))
    }

    linearized.mkString("\n")
  }

  private[this] def rec(depth: Int, r: HTree[String, Result])
                       (f: (Int, Either[String, Result]) => Unit): Unit = {
    r match{
      case HTree.Leaf(l) => f(depth, Right(l))
      case HTree.Node(v, c@_*) =>
        f(depth, Left(v))
        c.foreach(rec(depth+1, _)(f))
    }
  }
}

