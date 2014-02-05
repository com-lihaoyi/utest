package utest

object SkippedOuterFailure{
  def errorMsg(errorPath: Seq[String]) = {
    "Test skipped due to outer failure in " + errorPath.mkString(".")
  }
}
case class SkippedOuterFailure(errorPath: Seq[String],
                               outerError: Throwable)
                               extends Exception(SkippedOuterFailure.errorMsg(errorPath), outerError)

/**
 * Indicates that there was no test to run at the path you provided
 */
case class NoSuchTestException(path: String*) extends Exception("["+path.mkString(".") + "]")

/**
 * A special `AssertionError` thrown by utest's macro-powered asserts that 
 * contains metadata about local variables used in the assert expression.
 */
case class AssertionError(msg: String,
                          captured: Seq[LoggedValue],
                          cause: Throwable = null)
                          extends java.lang.AssertionError(msg, cause)

/**
 * Information about a value that was logged in one of the macro-powered 
 * `assert` functions
 */
case class LoggedValue(name: String, tpeName: String, value: Any)