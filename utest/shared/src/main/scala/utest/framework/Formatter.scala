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
  def formatTruncateHeight: Int = 15
  def formatWrapWidth: Int = 100

  def formatValue(x: Any) = testValueColor(x.toString)

  def toggledColor(t: fansi.Attrs) = if(formatColor) t else fansi.Attrs.Empty
  def testValueColor = toggledColor(fansi.Color.Blue)
  def exceptionClassColor = toggledColor(fansi.Underlined.On ++ fansi.Color.LightRed)
  def exceptionMsgColor = toggledColor(fansi.Color.LightRed)
  def exceptionPrefixColor = toggledColor(fansi.Color.Red)
  def exceptionMethodColor = toggledColor(fansi.Color.LightRed)
  def exceptionPunctuationColor = toggledColor(fansi.Color.Red)
  def exceptionLineNumberColor = toggledColor(fansi.Color.LightRed)

  def formatResultColor(success: Boolean) = toggledColor(
    if (success) fansi.Color.Green
    else fansi.Color.Red
  )

  def formatMillisColor = toggledColor(fansi.Bold.Faint)

  def formatException(x: Throwable, leftIndent: String) = {
    val output = mutable.Buffer.empty[fansi.Str]
    var current = x
    while(current != null){
      val exCls = exceptionClassColor(current.getClass.getName)
      output.append(
        joinLineStr(
          lineWrapInput(
            current.getMessage match{
              case null => exCls
              case nonNull => fansi.Str.join(exCls, ": ", exceptionMsgColor(nonNull))
            },
            leftIndent
          ),
          leftIndent
        )
      )

      val stack = current.getStackTrace


      StackMarker.filterCallStack(stack)
        .foreach { e =>
          // Scala.js for some reason likes putting in full-paths into the
          // filename slot, rather than just the last segment of the file-path
          // like Scala-JVM does. This results in that portion of the
          // stacktrace being terribly long, wrapping around and generally
          // being impossible to read. We thus manually drop the earlier
          // portion of the file path and keep only the last segment
          val shortenedFilename = e.getFileName.lastIndexOf('/') match{
            case -1 => e.getFileName
            case n => e.getFileName.drop(n + 1)
          }

          val frameIndent = leftIndent + "  "
          output.append(
            "\n", frameIndent,
            joinLineStr(
              lineWrapInput(

                fansi.Str.join(
                  exceptionPrefixColor(e.getClassName + "."),
                  exceptionMethodColor(e.getMethodName),
                  exceptionPunctuationColor("("),
                  exceptionLineNumberColor(shortenedFilename),
                  exceptionPunctuationColor(":"),
                  exceptionLineNumberColor(e.getLineNumber.toString),
                  exceptionPunctuationColor(")")
                ),
                frameIndent
              ),
              frameIndent
            )
          )
        }
      current = current.getCause
      if (current != null) output.append("\n", leftIndent)
    }

    fansi.Str.join(output:_*)
  }

  def lineWrapInput(input: fansi.Str, leftIndent: String): Seq[fansi.Str] = {
    val output = mutable.Buffer.empty[fansi.Str]
    val plainText = input.plainText
    var index = 0
    while(index < plainText.length){
      val nextWholeLine = index + (formatWrapWidth - leftIndent.length)
      val (nextIndex, skipOne) = plainText.indexOf('\n', index + 1) match{
        case -1 =>
          if (nextWholeLine < plainText.length) (nextWholeLine, false)
          else (plainText.length, false)
        case n =>
          if (nextWholeLine < n) (nextWholeLine, false)
          else (n, true)
      }

      output.append(input.substring(index, nextIndex))
      if (skipOne) index = nextIndex + 1
      else index = nextIndex
    }
    output
  }

  def joinLineStr(lines: Seq[fansi.Str], leftIndent: String) = {
    fansi.Str.join(lines.flatMap(Seq[fansi.Str]("\n", leftIndent, _)).drop(2):_*)
  }

  private[this] def prettyTruncate(r: Result, leftIndent: String): fansi.Str = {
    r.value match{
      case Success(()) => ""
      case Success(v) =>

        val wrapped = lineWrapInput(formatValue(v), leftIndent)
        val truncated =
          if (wrapped.length <= formatTruncateHeight) wrapped
          else wrapped.take(formatTruncateHeight) :+ testValueColor("...")

        joinLineStr(truncated, leftIndent)

      case Failure(e) => formatException(e, leftIndent)
    }
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
      if (lhs.length + rhs.length <= formatWrapWidth && !rhs.plainText.contains('\n')) " "
      else "\n" + leftIndent + "  "

    lhs ++ sep ++ rhs
  }

  def formatSingle(path: Seq[String], r: Result): Option[fansi.Str] = Some{
    wrapLabel(0, r, path.mkString("."))
  }

  def formatIcon(success: Boolean): fansi.Str = {
    formatResultColor(success)(if (success) "+" else "X")
  }

  def formatSummary(topLevelName: String, results: HTree[String, Result]): Option[fansi.Str] = Some{

    val relabelled = results match{
      case HTree.Node(v, c@_*) => HTree.Node(topLevelName, c:_*)
      case HTree.Leaf(r) => HTree.Leaf(r.copy(name = topLevelName))
    }
    val (rendered, totalTime) = rec(0, relabelled){
      case (depth, Left((name, millis))) =>
        fansi.Str("  " * depth + "- " + name + " ") ++ formatMillisColor(millis + "ms")
      case (depth, Right(r)) => wrapLabel(depth, r, r.name)
    }

    rendered.mkString("\n")
  }

  private[this] def rec(depth: Int, r: HTree[String, Result])
                       (f: (Int, Either[(String, Long), Result]) => fansi.Str): (Seq[fansi.Str], Long) = {
    r match{
      case HTree.Leaf(l) => (Seq(f(depth, Right(l))), l.milliDuration)
      case HTree.Node(v, c@_*) =>
        val (subStrs, subTimes) = c.map(rec(depth+1, _)(f)).unzip
        val cumulativeTime = subTimes.sum
        val thisStr = f(depth, Left(v, cumulativeTime))
        (thisStr +: subStrs.flatten, cumulativeTime)
    }
  }
}

