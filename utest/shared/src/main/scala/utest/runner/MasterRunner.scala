package utest
package runner
//import acyclic.file
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import sbt.testing.TaskDef

import scala.annotation.tailrec
object MasterRunner{
  def formatSummary(resultsHeader: fansi.Str,
                    body: fansi.Str,
                    failureMsg: fansi.Str,
                    successCount: Int,
                    failureCount: Int,
                    showSummaryThreshold: Int): fansi.Str = {
    val totalCount = successCount + failureCount
    val summary: fansi.Str =
      if (totalCount < showSummaryThreshold) ""
      else fansi.Str.join(
        resultsHeader, "\n",
        body, "\n",
        failureMsg, "\n"
      )
    fansi.Str.join(
      summary,
      s"Tests: ", totalCount.toString, ", ",
      s"Passed: ", successCount.toString, ", ",
      s"Failed: ", failureCount.toString
    ).render
  }
}
final class MasterRunner(args: Array[String],
                         remoteArgs: Array[String],
                         testClassLoader: ClassLoader,
                         setup: () => Unit,
                         teardown: () => Unit,
                         showSummaryThreshold: Int,
                         startHeader: String => String,
                         resultsHeader: String,
                         failureHeader: String,
                         useSbtLoggers: Boolean)
                         extends BaseRunner(args, remoteArgs, testClassLoader, useSbtLoggers){

  println(startHeader(path.fold("")(" " + _)))
  setup()
  val results = new AtomicReference[List[String]](Nil)
  val success = new AtomicInteger(0)
  val failure = new AtomicInteger(0)
  val failures = new AtomicReference[List[String]](Nil)
  val traces = new AtomicReference[List[String]](Nil)

  @tailrec def addResult(r: String): Unit = {
    val old = results.get()
    if (!results.compareAndSet(old, r :: old)) addResult(r)
  }

  @tailrec final def addFailure(r: String): Unit = {
    val old = failures.get()
    if (!failures.compareAndSet(old, r :: old)) addFailure(r)
  }
  @tailrec final def addTrace(r: String): Unit = {
    val old = traces.get()
    if (!traces.compareAndSet(old, r :: old)) addTrace(r)
  }

  def incSuccess(): Unit = success.incrementAndGet()
  def incFailure(): Unit = failure.incrementAndGet()

  def successCount: Int = success.get
  def failureCount: Int = failure.get

  def done(): String = {
    teardown()
    val total = success.get() + failure.get()
    /**
      * Print out the results summary ourselves rather than returning it from
      * `done`, to work around https://github.com/sbt/sbt/issues/3510
      */
    if (total > 0) {
      MasterRunner.formatSummary(
        resultsHeader = resultsHeader,
        body = results.get.mkString("\n"),
        failureMsg =
          if (failures.get() == Nil) ""
          else fansi.Str(failureHeader) ++ fansi.Str.join(
            // reverse, because the list gets accumulated backwards
            failures.get().reverse.flatMap(Seq[fansi.Str]("\n", _)): _*
          ),
        successCount = success.get(),
        failureCount = failure.get(),
        showSummaryThreshold = showSummaryThreshold
      )



      println(

      )
    }
    // Don't print anything, but also don't print the default message it
    // normally prints if you return an empty string, and don't print the
    // [info] gutter it prints if you return " "
    "\n"
  }

  def receiveMessage(msg: String): Option[String] = {
    def badMessage = sys.error("bad message: " + msg)
    msg(0) match {
      case 'h' => // hello message. nothing special to do
      case 'r' => addResult(msg.tail)
      case 'f' => addFailure(msg.tail)
      case 'c' => addTrace(msg.tail)
      case 'i' => msg(1) match {
        case 's' => incSuccess()
        case 'f' => incFailure()
        case _ => badMessage
      }
      case _ => badMessage
    }

    val countMsg = s"$successCount,$failureCount"
    Some(countMsg)
  }

}

