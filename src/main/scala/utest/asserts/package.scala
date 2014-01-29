package utest


package object asserts {
  case class LoggedAssertionError(msg: String, captured: Seq[LoggedValue]) extends AssertionError(msg)

  case class LoggedValue(name: String, tpeName: String, value: Any)
}
