package utest.runner
import sbt.testing._
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.annotation.tailrec

trait GenericRunner extends sbt.testing.Runner{

  val args: Array[String]
  val remoteArgs: Array[String]

  val results = new AtomicReference[List[String]](Nil)
  val total = new AtomicInteger(0)
  val success = new AtomicInteger(0)
  val failure = new AtomicInteger(0)

  @tailrec final def addResult(r: String): Unit = {
    val old = results.get()
    if (!results.compareAndSet(old, r :: old)) addResult(r)
  }

  def doStuff(selector: Seq[String], loggers: Seq[Logger], name: String): Unit

  def progressString = {
    s"${success.get + failure.get}/${total.get}".padTo(8, ' ')
  }

  def tasks(taskDefs: Array[TaskDef]) = {
    val path = args.lift(0)
      .filter(_(0) != '-')
      .getOrElse("")

    taskDefs.map(t => new Task(t, args, path, doStuff): sbt.testing.Task)
  }


  def done(): String = {
    val header = "-----------------------------------Results-----------------------------------"

    val body = results.get.mkString("\n")

    if (failure.get() != 0 && args.contains("--throw")) throw new Exception("Tests Failed")
    Seq(
      header,
      body,
      s"Tests: $total",
      s"Passed: $success",
      s"Failed: $failure"
    ).mkString("\n")
  }
}

