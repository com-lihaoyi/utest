package utest
package runner
//import acyclic.file

/**
  * Stub runner for Scala.js only which runs tasks but forwards the results to
  * the master over `send`, for the master to aggregate and display
  */
final class ScalaJsSlaveRunner(args: Array[String],
                               remoteArgs: Array[String],
                               testClassLoader: ClassLoader,
                               send: String => Unit,
                               setup: () => Unit,
                               teardown: () => Unit,
                               useSbtLoggers: Boolean,
                               formatter: utest.framework.Formatter)
                        extends BaseRunner(args, remoteArgs, testClassLoader, useSbtLoggers, formatter){
  setup()
  def addResult(r: String): Unit = send(s"r$r")
  def addFailure(r: String): Unit = send(s"f$r")
  def addTrace(trace: String): Unit = send(s"c$trace")
  def incSuccess() = send(s"is")
  def incFailure() = send(s"if")

  // These only exist because Scala.js is weird and asks us to define them
  // even though we never end up using them, so stub them out
  def done() = {
    teardown()
    ""
  }
  def receiveMessage(msg: String) = None
}
