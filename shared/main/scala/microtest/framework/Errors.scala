package microtest.framework

case class SkippedDueToOuterFailureError(errorPath: Seq[String],
                                         outerError: Throwable)
  extends Exception("Test skipped due to outer failure in " + errorPath.mkString("."), outerError)

case class NoSuchTestException(path: String*) extends Exception("["+path.mkString(".") + "]")