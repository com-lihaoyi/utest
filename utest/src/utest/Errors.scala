package utest

import utest.shaded._
import utest.shaded.fansi.{Attrs, Str}

//import acyclic.file

/**
 * Indicates that there was no test to run at the path you provided
 */
case class NoSuchTestException(path: Seq[String]*)
  extends Exception(path.map(_.mkString(".")).mkString("[", ", ", "]"))

/**
 * A special `AssertionError` thrown by utest's macro-powered asserts that
 * contains metadata about local variables used in the assert expression.
 */
case class AssertionError(msgPrefix: String, captured: Seq[TestValue], cause: Throwable = null)
extends java.lang.AssertionError(
  AssertionError.render(
    msgPrefix,
    captured,
    new AssertionError.Printer{
      def pprinter(v: Any): Str = pprint.PPrinter.BlackWhite.apply(v)
      def nameColor: Attrs = fansi.Attrs.Empty
      def colored = false
    }
  ).plainText
)
  with ColorMessageError {
  super.initCause(cause)
  override def coloredMessage(pprinter: AssertionError.Printer): fansi.Str = AssertionError.render(msgPrefix, captured, pprinter)
}

object AssertionError {
  trait Printer {
    def pprinter(v: Any): fansi.Str
    def nameColor: fansi.Attrs
    def colored: Boolean

  }
  def diff(lhs: fansi.Str, rhs: fansi.Str) = {
    def splitLines(str: fansi.Str): IndexedSeq[fansi.Str] = {
      val lines = str.plainText.linesWithSeparators.toList
    
      // Compute array of starting offsets for each line
      val result = new Array[fansi.Str](lines.length)
      var offset = 0
      var i = 0
    
      while (i < lines.length) {
        val len = lines(i).length
        result(i) = str.substring(offset, offset + len)
        offset += len
        i += 1
      }
    
      result.toIndexedSeq
    }

    import app.tulz.diff._
    val lhsLines = splitLines(lhs)
    val rhsLines = splitLines(rhs)
    val diffElements = SeqDiff.seq(lhsLines, rhsLines)

    def wrap(s: fansi.Str): Seq[fansi.Str] = {
      if (s.plainText.lastOption == Some('\n')) Seq[fansi.Str](s) else Seq(s ++ fansi.Str("\n"))
    }

    def render0(prefix: String, x: Seq[fansi.Str], color: fansi.Attrs) =
      x.flatMap(s => Seq(color(prefix)) ++ wrap(color(s)))

    def renderRed(x: Seq[fansi.Str]) =
      render0("- ", x, fansi.Attrs(fansi.Color.Reset, fansi.Back.Red))

    def renderGreen(x: Seq[fansi.Str]) =
      render0("+ ", x, fansi.Attrs(fansi.Color.Reset, fansi.Back.Green))

    diffElements.flatMap {
      case DiffElement.InBoth(x) => render0("  ", x, fansi.Attrs.Empty)
      case DiffElement.InFirst(x) => renderRed(x)
      case DiffElement.InSecond(x) => renderGreen(x)
      case DiffElement.Diff(x, y) =>
        // Within any adjacent blocks of differing text, do a char-by-char
        // diff between the two versions so we can underline the characters
        // that differ between them
        val subDiff = StringDiff.diff(x.map(_.plainText).mkString, y.map(_.plainText).mkString)
        val sectionsX = subDiff.collect{
          case DiffElement.InBoth(x) => (false, x.length)
          case DiffElement.InFirst(x) => (true, x.length)
          case DiffElement.Diff(x, y) => (true, x.length)
        }

        val sectionsY = subDiff.collect{
          case DiffElement.InBoth(x) => (false, x.length)
          case DiffElement.InSecond(x) => (true, x.length)
          case DiffElement.Diff(x, y) => (true, y.length)
        }

        def sectionsToOverlays(sections: Seq[(Boolean, Int)]) = {
          val buffer = collection.mutable.Buffer.empty[(fansi.Attrs, Int, Int)]
          var index = 0
          for ((underline, length) <- sections) {
            val nextIndex = index + length
            if (underline) buffer.append((fansi.Underlined.On, index, nextIndex))
            index = nextIndex
          }
          buffer.toSeq
        }

        val overlaysX = sectionsToOverlays(sectionsX)
        val overlaysY = sectionsToOverlays(sectionsY)

        renderRed(splitLines(fansi.Str.join(x).overlayAll(overlaysX))) ++
          renderGreen(splitLines(fansi.Str.join(y).overlayAll(overlaysY)))
    }
  }

  def render(msgPrefix: String, captured: Seq[TestValue], pprinter: AssertionError.Printer) = {
    shaded.fansi.Str.join(
      Seq[shaded.fansi.Str](msgPrefix) ++
        captured.flatMap{
          case x: TestValue.Single =>
            Seq[fansi.Str](
              "\n",
              pprinter.nameColor(x.name),
              ": ",
              if (pprinter.colored) x.tpeName.getOrElse("") else fansi.Color.Reset(x.tpeName.getOrElse("")),
              " = ",
              pprinter.pprinter(x.value
              )
            )

          case x: TestValue.Equality =>
            Seq[fansi.Str](pprinter.nameColor(s"\n${x.lhs.name} != ${x.rhs.name}"), ":\n") ++
              diff(pprinter.pprinter(x.lhs.value), pprinter.pprinter(x.rhs.value))
        }
    )
  }
}

trait ColorMessageError {
  def coloredMessage(pprinter: AssertionError.Printer): fansi.Str
}

/**
 * Information about a value that was logged in one of the macro-powered
 * `assert` functions
 */
sealed trait TestValue
object TestValue {
  case class Single(name: String, tpeName: Option[fansi.Str], value: Any) extends TestValue
  case class Equality(lhs: Single, rhs: Single) extends TestValue
}

/**
 * Simplified versions of the errors thrown during compilation, for use with the
 * [[utest.asserts.Asserts.assertCompileError]] macro. Contains only a single message and no position since
 * things compiled using macros don't really have source positions.
 */
trait CompileError extends CompileErrorVersionSpecific {
  def pos: String
  def msg: String

  /**
   * Performs some basic, common checking on the compilation error object,
   * to verify that it matches what you expect
   *
   * @param errorPos The expected position-message returned by the compile
   *                 error. Usually something like
   *
   * """
   * true * false
   *      ^
   * """
   *
   * This mimicks the position-message shown in the terminal, and should be a
   * convenient way of indicating where you expect the error to occur. Pass
   * in an empty-string to skip this check.
   *
   * @param msgs A list of snippets that should appear in the error message.
   *             Typically something like "value * is not a member of Boolean"
   *             to ensure that the message is what you want
   */
  def check(errorPos: String, msgs: String*) = {
    val stripped = errorPos.reverse.dropWhile("\n ".toSet.contains).reverse
    if (errorPos != "") Predef.assert(
      normalizedPos == stripped,
      "Compile error positions do not match\n" +
      "Expected Position\n" +
      stripped + "\n" +
      "Actual Position\n" +
      normalizedPos
    )
    for(msg <- msgs){
      Predef.assert(
        this.msg.contains(msg),
        "Error message did not contain expected snippet\n" +
        "Error message\n" +
        this.msg + "\n" +
        "Expected Snippet\n" +
        msg
      )
    }
  }
}

object CompileError{

  /**
   * A [[CompileError]] representing a failure to typecheck.
   */
  case class Type(pos: String, msg: String) extends CompileError
  /**
   * A [[CompileError]] representing a failure to parse.
   */
  case class Parse(pos: String, msg: String) extends CompileError
  /**
    * A [[CompileError]] representing a `compileTimeOnly` node within a tree
    */
  case class CompileTimeOnly(pos: String, msg: String) extends CompileError
}
