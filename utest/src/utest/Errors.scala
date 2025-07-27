package utest

import utest.shaded._

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
extends java.lang.AssertionError(AssertionError.render(msgPrefix, captured).plainText)
  with ColorMessageError {
  super.initCause(cause)
  override def coloredMessage: fansi.Str = AssertionError.render(msgPrefix, captured)
}

object AssertionError {
  def diff(lhs: fansi.Str, rhs: fansi.Str) = {
    def splitLines(str: fansi.Str): IndexedSeq[fansi.Str] = {
      val lineLengths = str.plainText.linesWithSeparators.map(_.length).toList
      (Seq(0) ++ lineLengths).inits.toList.reverse
        .sliding(2)
        .drop(1)
        .collect { case Seq(start, end) => str.substring(start.sum, end.sum) }
        .toIndexedSeq
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
      case DiffElement.Diff(x, y) => renderRed(x) ++ renderGreen(y)
    }
  }
  def render(msgPrefix: String, captured: Seq[TestValue]) = {
    shaded.fansi.Str.join(
      Seq[shaded.fansi.Str](msgPrefix) ++
        captured.flatMap{
          case x: TestValue.Single =>
            Seq[fansi.Str]("\n", fansi.Color.Cyan(x.name), ": ", x.tpeName.getOrElse(""), " = ", shaded.pprint.apply(x.value))

          case x: TestValue.Equality =>
            Seq[fansi.Str](fansi.Color.Cyan(s"\n${x.lhs.name} != ${x.rhs.name}"), ":\n") ++
              diff(shaded.pprint.apply(x.lhs.value), shaded.pprint.apply(x.rhs.value))
        }
    )
  }
}

trait ColorMessageError {
  def coloredMessage: shaded.fansi.Str
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
 * [[utest.asserts.Asserts.compileError]] macro. Contains only a single message and no position since
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
