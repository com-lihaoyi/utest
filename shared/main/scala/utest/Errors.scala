package utest

case class SkippedDueToOuterFailureError(errorPath: Seq[String],
                                         outerError: Throwable)
  extends Exception("Test skipped due to outer failure in " + errorPath.mkString("."), outerError)

case class NoSuchTestException(path: String*) extends Exception("["+path.mkString(".") + "]")

case class LoggedAssertionError(msg: String, captured: Seq[LoggedValue]) extends AssertionError(msg)

case class LoggedValue(name: String, tpeName: String, value: Any)