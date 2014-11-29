package utest.runner

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

  @tailrec final def addResult(r: String): Unit = {
    val old = results.get()
    if (!results.compareAndSet(old, r :: old)) addResult(r)
  }

  @tailrec final def addFailure(r: String): Unit = {
    val old = failures.get()
    if (!failures.compareAndSet(old, r :: old)) addFailure(r)
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

    if (failure.get() != 0 && args.contains("--throw")) throw new Exception("Tests Failed")
    else if (total.get == 1) " " // can't be empty because then SBT will fill it with trash
    else {
      Seq(
        header,
        body,
        "Failures:",
        failures.get().mkString("\n"),
        s"Tests: $total",
        s"Passed: $success",
        s"Failed: $failure"
      ).mkString("\n")
    }

  }

  def receiveMessage(msg: String): Option[String] = {
    def badMessage = sys.error("bad message: " + msg)
    msg(0) match {
      case 'h' => // hello message. nothing special to do
      case 'r' =>
        addResult(msg.tail)
      case 'f' =>
        addFailure(msg.tail)
      case 't' =>
        addTotal(msg.tail.toInt)
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

