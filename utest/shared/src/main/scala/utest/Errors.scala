package utest

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
extends java.lang.AssertionError(
  msgPrefix + captured.map(x => s"\n${x.name}: ${x.tpeName} = ${x.value}").mkString
) {
  super.initCause(cause)
}

/**
 * Information about a value that was logged in one of the macro-powered
 * `assert` functions
 */
case class TestValue(name: String, tpeName: String, value: Any)

/**
 * Simplified versions of the errors thrown during compilation, for use with the
 * [[utest.asserts.Asserts.compileError]] macro. Contains only a single message and no position since
 * things compiled using macros don't really have source positions.
 */
trait CompileError{
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
    val normalizedPos = "\n" + pos
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
