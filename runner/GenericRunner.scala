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
  val failures = new AtomicReference[List[String]](Nil)

  @tailrec final def addResult(r: String): Unit = {
    val old = results.get()
    if (!results.compareAndSet(old, r :: old)) addResult(r)
  }
  @tailrec final def addFailure(r: String): Unit = {
    val old = failures.get()
    if (!failures.compareAndSet(old, r :: old)) addFailure(r)
  }

  /**
   * Actually performs the running of a particular test; this method is
   * abstract an intended to be overriden by each implementation, since
   * e.g. running a test on the JVM is much different from running a test
   * in Javascript using Rhino/NodeJS/PhantomJS
   *
   * @param selector The name of the test within the test class/object
   * @param loggers
   * @param name The name of the test class/object
   */
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
      "Failures:",
      failures.get().mkString("\n"),
      s"Tests: $total",
      s"Passed: $success",
      s"Failed: $failure"
    ).mkString("\n")
  }
}

