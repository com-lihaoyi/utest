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
  def formatWrapWidth: Int = Int.MaxValue >> 1 // halving here to avoid overflows later

  def formatValue(x: Any) = testValueColor("" + x)

  def toggledColor(t: utest.fansi.Attrs) = if(formatColor) t else utest.fansi.Attrs.Empty
  def testValueColor = toggledColor(utest.fansi.Color.Blue)
  def exceptionClassColor = toggledColor(utest.fansi.Underlined.On ++ utest.fansi.Color.LightRed)
  def exceptionMsgColor = toggledColor(utest.fansi.Color.LightRed)
  def exceptionPrefixColor = toggledColor(utest.fansi.Color.Red)
  def exceptionMethodColor = toggledColor(utest.fansi.Color.LightRed)
  def exceptionPunctuationColor = toggledColor(utest.fansi.Color.Red)
  def exceptionLineNumberColor = toggledColor(utest.fansi.Color.LightRed)

  def formatResultColor(success: Boolean) = toggledColor(
    if (success) utest.fansi.Color.Green
    else utest.fansi.Color.Red
  )

  def formatMillisColor = toggledColor(utest.fansi.Bold.Faint)

  def exceptionStackFrameHighlighter(s: StackTraceElement): Boolean = true

  def formatException(x: Throwable, leftIndent: String) = {
    val output = mutable.Buffer.empty[utest.fansi.Str]
    var current = x
    while(current != null){
      val exCls = exceptionClassColor(current.getClass.getName)
      output.append(
        joinLineStr(
          lineWrapInput(
            current.getMessage match{
              case null => exCls
              case nonNull => utest.fansi.Str.join(Seq(exCls, ": ", exceptionMsgColor(nonNull)))
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

          val filenameFrag: utest.fansi.Str = e.getFileName match{
            case null => exceptionLineNumberColor("Unknown")
            case fileName =>
              val shortenedFilename = fileName.lastIndexOf('/') match{
                case -1 => fileName
                case n => fileName.drop(n + 1)
              }
              utest.fansi.Str.join(Seq(
                exceptionLineNumberColor(shortenedFilename),
                ":",
                exceptionLineNumberColor(e.getLineNumber.toString)
              ))
          }

          val frameIndent = leftIndent + "  "
          val wrapper =
            if(exceptionStackFrameHighlighter(e)) utest.fansi.Attrs.Empty
            else utest.fansi.Bold.Faint

          output.append(
            "\n", frameIndent,
            joinLineStr(
              lineWrapInput(
                wrapper(
                  utest.fansi.Str.join(Seq(
                    exceptionPrefixColor(e.getClassName + "."),
                    exceptionMethodColor(e.getMethodName),
                    exceptionPunctuationColor("("),
                    filenameFrag,
                    exceptionPunctuationColor(")")
                  ))
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

    utest.fansi.Str.join(output.toSeq)
  }

  def lineWrapInput(input: utest.fansi.Str, leftIndent: String): Seq[utest.fansi.Str] = {
    val output = mutable.Buffer.empty[utest.fansi.Str]
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
    output.toSeq
  }

  def joinLineStr(lines: Seq[utest.fansi.Str], leftIndent: String) = {
    utest.fansi.Str.join(lines.flatMap(Seq[utest.fansi.Str]("\n", leftIndent, _)).drop(2))
  }

  private[this] def prettyTruncate(r: Result, leftIndent: String): utest.fansi.Str = {
    r.value match{
      case Success(()) => ""
      case Success(v) =>
        val wrapped = lineWrapInput(pprint.apply(v, height = formatTruncateHeight).overlay(fansi.Color.Blue), leftIndent)
        joinLineStr(wrapped, leftIndent)

      case Failure(e) => formatException(e, leftIndent)
    }
  }

  def wrapLabel(leftIndentCount: Int, r: Result, label: String): utest.fansi.Str = {
    val leftIndent = "  " * leftIndentCount
    val lhs = utest.fansi.Str.join(Seq(
      leftIndent,
      formatIcon(r.value.isInstanceOf[Success[_]]), " ",
      label, " ",
      formatMillisColor(r.milliDuration + "ms"), " "
    ))

    val rhs = prettyTruncate(r, leftIndent + "  ")

    val sep =
      if (lhs.length + rhs.length <= formatWrapWidth && !rhs.plainText.contains('\n')) " "
      else "\n" + leftIndent + "  "

    lhs ++ sep ++ rhs
  }

  def formatSingle(path: Seq[String], r: Result): Option[utest.fansi.Str] = Some{
    wrapLabel(0, r, path.mkString("."))
  }

  def formatIcon(success: Boolean): utest.fansi.Str = {
    formatResultColor(success)(if (success) "+" else "X")
  }

  def formatSummary(topLevelName: String, results: HTree[String, Result]): Option[utest.fansi.Str] = Some{

    val relabelled = results match{
      case HTree.Node(v, c@_*) => HTree.Node(topLevelName, c:_*)
      case HTree.Leaf(r) => HTree.Leaf(r.copy(name = topLevelName))
    }
    val (rendered, totalTime) = rec(0, relabelled){
      case (depth, Left((name, millis))) =>
        utest.fansi.Str("  " * depth + "- " + name + " ") ++ formatMillisColor(millis + "ms")
      case (depth, Right(r)) => wrapLabel(depth, r, r.name)
    }

    rendered.mkString("\n")
  }

  private[this] def rec(depth: Int, r: HTree[String, Result])
                       (f: (Int, Either[(String, Long), Result]) => utest.fansi.Str): (Seq[utest.fansi.Str], Long) = {
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

