package utest
package framework
//import acyclic.file
import scala.collection.mutable
import scala.util.{Failure, Success}

/**
 * Default implementation of [[Formatter]], also used by the default SBT test
 * framework. Allows some degree of customization of the formatted test results.
 */
trait Formatter {

  def formatColor: Boolean = true
  def formatTruncate: Int = 5000
  def formatTrace: Boolean = true
  def formatWrapThreshold: Int = 90

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

  def wrapLabel(leftIndentCount: Int, r: Result, label: String, isLeaf: Boolean): fansi.Str = {
    val leftIndent = "  " * leftIndentCount
    if (!isLeaf) leftIndent + "- " + label
    else {
      val lhs = fansi.Str.join(
        leftIndent,
        formatIcon(r.value.isInstanceOf[Success[_]]), " ",
        label, " ",
        formatMillisColor(r.milliDuration + "ms"), " "
      )

      val rhs = prettyTruncate(r, e => formatResultColor(false)(e.toString))

      val sep =
        if (lhs.length + rhs.length <= formatWrapThreshold) " "
        else "\n" + leftIndent + "  "

      lhs ++ sep ++ rhs
    }
  }

  def formatSingle(path: Seq[String], r: Result): Option[fansi.Str] = Some{
    wrapLabel(0, r, path.mkString("."), true)
  }

  def formatIcon(success: Boolean): fansi.Str = {
    formatResultColor(success)(if (success) "+" else "X")
  }

  def format(topLevelName: String, results: Tree[Result]): Option[fansi.Str] = Some{
    val linearized = mutable.Buffer.empty[(Int, Result, Boolean)]

    rec(0, Tree(results.value.copy(name = topLevelName), results.children:_*)){
      (depth, r, isLeaf) => linearized.append((depth, r, isLeaf))
    }

    linearized
      .map{case (depth, r, isLeaf) => wrapLabel(depth, r, r.name, isLeaf)}
      .mkString("\n")
  }

  private[this] def rec(depth: Int, r: Tree[Result])(f: (Int, Result, Boolean) => Unit): Unit = {
    f(depth, r.value, r.children.isEmpty)
    r.children.foreach(rec(depth+1, _)(f))
  }
}

