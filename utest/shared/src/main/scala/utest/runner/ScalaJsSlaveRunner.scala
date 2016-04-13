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
                               teardown: () => Unit)
                        extends BaseRunner(args, remoteArgs, testClassLoader){
  setup()
  def addResult(r: String): Unit = send(s"r$r")
  def addFailure(r: String): Unit = send(s"f$r")
  def addTrace(trace: String): Unit = send(s"c$trace")
  def addTotal(v: Int) = send(s"t$v")
  def incSuccess() = send("is")
  def incFailure() = send("if")
  def logDuration(ms: Long, name: String) = send("d" + ms + "|" + name)

  // These only exist because Scala.js is weird and asks us to define them
  // even though we never end up using them, so stub them out
  def done() = {
    teardown()
    ""
  }
  def receiveMessage(msg: String) = None
}
