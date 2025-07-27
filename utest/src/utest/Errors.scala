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
// Referring to non-existent method java.lang.AssertionError.<init>(java.lang.String,java.lang.Throwable) in Scala.js 1.0.0-M1
//                          extends java.lang.AssertionError(msg, cause) {
extends java.lang.AssertionError(AssertionError.render(msgPrefix, captured).plainText)
  with ColorMessageError {
  super.initCause(cause)
  override def coloredMessage: fansi.Str = AssertionError.render(msgPrefix, captured)
}

object AssertionError {
  def render(msgPrefix: String, captured: Seq[TestValue]) = {
    shaded.fansi.Str.join(
      Seq[shaded.fansi.Str](msgPrefix) ++
        captured.flatMap[shaded.fansi.Str]{
          case x: TestValue.Single =>
            Seq(s"\n${x.name}: ${x.tpeName} = ", shaded.pprint.apply(x.value))
          case x: TestValue.Equality =>
            def splitLines(str: fansi.Str): IndexedSeq[fansi.Str] = {
              val lineLengths = str.plainText.linesWithSeparators.map(_.length).toList
              (Seq(0) ++ lineLengths).inits.toList.reverse
                .sliding(2)
                .collect{case Seq(start, end) => str.substring(start.sum, end.sum)}
                .toIndexedSeq
            }

            import app.tulz.diff._
            val lhsLines = splitLines(shaded.pprint.apply(x.lhs.value))
            val rhsLines = splitLines(shaded.pprint.apply(x.rhs.value))
            val diffElements = SeqDiff.seq(lhsLines, rhsLines)

            def wrap(s: fansi.Str): Seq[fansi.Str] = {
              if (s.plainText.lastOption == Some('\n')) Seq(s) else Seq(s ++ fansi.Str("\n"))
            }

            Seq(fansi.Str("\n")) ++ diffElements.flatMap[fansi.Str]{
              case DiffElement.InBoth(x) => x.flatMap(Seq(fansi.Str("  ")) ++ wrap(_))
              case DiffElement.InFirst(x) => x.flatMap(Seq(fansi.Color.Red("-"), fansi.Str(" ")) ++ wrap(_))
              case DiffElement.InSecond(x) => x.flatMap(Seq(fansi.Color.Green("+"), fansi.Str(" ")) ++ wrap(_))
              case DiffElement.Diff(x, y) => x.flatMap(Seq(fansi.Color.Red("-"), fansi.Str(" ")) ++ wrap(_)) ++ y.flatMap(Seq(fansi.Color.Green("+"), fansi.Str(" ")) ++ wrap(_))
            }
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
  case class Single(name: String, tpeName: String, value: Any) extends TestValue
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
