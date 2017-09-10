package utest.framework

/**
  * Wrapper-functions that can be used to mark parts of the callstack that are
  * meant to be filtered out later.
  */
object StackMarker {
  def dropInside[T](t: => T): T = t
  def dropOutside[T](t: => T): T = t
  def filterCallStack(stack: Seq[StackTraceElement]): Seq[StackTraceElement] = {
    val droppedInside = stack.indexWhere(x =>
      x.getClassName == "utest.framework.StackMarker$" && x.getMethodName == "dropInside"
    )

    val droppedOutside = stack.indexWhere(x =>
      x.getClassName == "utest.framework.StackMarker$" && x.getMethodName == "dropOutside"
    )

    stack.slice(
      droppedInside match {case -1 => 0 case n => n + 2},
      droppedOutside match {case -1 => stack.length case n => n}
    )
  }
}
