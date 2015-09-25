package utest.runner
import acyclic.file
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import scala.annotation.tailrec

final class MasterRunner(args: Array[String],
                         remoteArgs: Array[String],
                         testClassLoader: ClassLoader)
                         extends BaseRunner(args, remoteArgs, testClassLoader){

  val results = new AtomicReference[List[String]](Nil)
  val total = new AtomicInteger(0)
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

  def addTotal(v: Int): Unit = total.addAndGet(v)
  def incSuccess(): Unit = success.incrementAndGet()
  def incFailure(): Unit = failure.incrementAndGet()

  def successCount: Int = success.get
  def failureCount: Int = failure.get
  def totalCount: Int = total.get

  def done(): String = {
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
    Seq(
      header,
      body,
      failureMsg,
      s"Tests: $total",
      s"Passed: $success",
      s"Failed: $failure"
    ).mkString("\n")
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
        case 's' => incSuccess
        case 'f' => incFailure
        case _ => badMessage
      }
      case _ => badMessage
    }

    val countMsg = s"$successCount,$failureCount,$totalCount"
    Some(countMsg)
  }

}

