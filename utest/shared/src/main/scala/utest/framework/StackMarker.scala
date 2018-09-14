package utest.framework

/**
  * Wrapper-functions that can be used to mark parts of the callstack that are
  * meant to be filtered out later.
  */
object StackMarker {
  // Ask Scalac/Scala.js nicely to try and avoid inlining these two marker methods,
  // to make sure they don't disappear from the stack traces
  @noinline
  def dropInside[T](t: => T): T = t
  @noinline
  def dropOutside[T](t: => T): T = t
  def filterCallStack(stack: Seq[StackTraceElement]): Seq[StackTraceElement] = {
    val droppedInside = stack.indexWhere(x =>
      x.getClassName == "utest.framework.StackMarker$" && x.getMethodName == "dropInside"
    )

    val droppedOutside = stack.indexWhere(x =>
      x.getClassName == "utest.framework.StackMarker$" && x.getMethodName == "dropOutside"
    )

    val stack1 = stack.slice(
      droppedInside match {case -1 => 0 case n => n + 2},
      droppedOutside match {case -1 => stack.length case n => n}
    )

    val lastNonLMFIndex = stack1.lastIndexWhere(x =>
      !x.getClassName.contains("$$Lambda$")
    )

    if (lastNonLMFIndex < 0) stack1
    else stack1.take(lastNonLMFIndex + 1)
  }
}
