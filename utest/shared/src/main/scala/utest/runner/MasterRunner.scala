package utest
package runner
//import acyclic.file
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.annotation.tailrec
import MasterRunner._

object MasterRunner {
  val DurAndName = "^.(\\d+?)\\|(.*)$".r
}

final class MasterRunner(args: Array[String],
                         remoteArgs: Array[String],
                         testClassLoader: ClassLoader,
                         setup: () => Unit,
                         teardown: () => Unit)
                         extends BaseRunner(args, remoteArgs, testClassLoader){
  setup()
  val results = new AtomicReference[List[String]](Nil)
  val total = new AtomicInteger(0)
  val success = new AtomicInteger(0)
  val failure = new AtomicInteger(0)
  val failures = new AtomicReference[List[String]](Nil)
  val traces = new AtomicReference[List[String]](Nil)
  val durations = new AtomicReference[Vector[(Long, String)]](Vector.empty)

  override def logDuration(ms: Long, name: String): Unit = {
    val pair = (ms, name)
    @tailrec def go: Unit = {
      val old = durations.get()
      if (!durations.compareAndSet(old, old :+ pair)) go
    }
    go
  }

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

  def addTotal(v: Int): Unit = total.addAndGet(v)
  def incSuccess(): Unit = success.incrementAndGet()
  def incFailure(): Unit = failure.incrementAndGet()

  def successCount: Int = success.get
  def failureCount: Int = failure.get
  def totalCount: Int = total.get

  def done(): String = {
    teardown()
    val header = "-----------------------------------Results-----------------------------------"

    val body = results.get.mkString("\n")

    val failureMsg = if (failures.get() == Nil) ""
    else Seq(
      Console.RED + "Failures:",
      failures.get()
              .zip(traces.get())
              // We pre-pending to a list, so need to reverse to make the order correct
              .reverse
              // ignore those with an empty trace, e.g. utest.SkippedOuterFailures,
              // since those are generally just spam (we already can see the outer failure)
              .collect{case (f, t) if t != "" => f + ("\n" + t).replace("\n", "\n"+Console.RED)}
              .mkString("\n")
    ).mkString("\n")

    val sb = new StringBuilder
    @inline def eol(): Unit = sb append '\n'
    sb append header
    eol(); sb append body
    eol(); sb append failureMsg
    eol(); sb append "Tests: "; sb append total
    eol(); sb append "Passed: "; sb append success
    eol(); sb append "Failed: "; sb append failure

    for (n <- reportSlowest) {
      val vec = durations.get()
      if (vec.nonEmpty) {
        val array = vec.toArray
        scala.util.Sorting.quickSort(array)(Ordering.by((_: (Long, String))._1).reverse)
        def slowestN = array.iterator.take(n)
        val nWidth = (n min array.length).toString.length
        val msWidth = "%,d".format(array.head._1).length
        val fmt = s"\n  #%${nWidth}d: (%,${msWidth}d ms) %s"
        eol(); sb append s"Slowest $n tests:"
        var i = 0
        for ((ms, name) <- slowestN) {
          i += 1
          sb append fmt.format(i, ms, name)
        }
      }
    }

    sb.toString()
  }

  def receiveMessage(msg: String): Option[String] = {
    def badMessage = sys.error("bad message: " + msg)
    msg(0) match {
      case 'h' => // hello message. nothing special to do
      case 'r' => addResult(msg.tail)
      case 'f' => addFailure(msg.tail)
      case 't' => addTotal(msg.tail.toInt)
      case 'c' => addTrace(msg.tail)
      case 'i' => msg(1) match {
        case 's' => incSuccess()
        case 'f' => incFailure()
        case _ => badMessage
      }
      case 'd' => msg match {
        case DurAndName(ms, name) => logDuration(ms.toLong, name)
        case _ => badMessage
      }
      case _ => badMessage
    }

    val countMsg = s"$successCount,$failureCount,$totalCount"
    Some(countMsg)
  }

}

