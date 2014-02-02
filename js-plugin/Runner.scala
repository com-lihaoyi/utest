import sbt.testing._
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.annotation.tailrec
import scala.scalajs.sbtplugin.ScalaJSEnvironment

class Runner(val args: Array[String],
             val remoteArgs: Array[String],
             environment: ScalaJSEnvironment)
             extends sbt.testing.Runner{

  val results = new AtomicReference[List[String]](Nil)
  val total = new AtomicInteger(0)
  val success = new AtomicInteger(0)
  def failure = total.get - success.get

  @tailrec final def addResult(r: String): Unit = {
    val old = results.get()
    if (!results.compareAndSet(old, r :: old)) addResult(r)
  }

  def tasks(taskDefs: Array[TaskDef]): Array[sbt.testing.Task] = {
    val path = args.lift(0)
                   .filter(_(0) != '-')
                   .getOrElse("")

    for(taskDef <- taskDefs) yield {
      new Task(
        taskDef,
        args,
        path,
        environment,
        (x, y) => {total.addAndGet(x); success.addAndGet(y)},
        addResult
      )
    }
  }

  def done(): String = {
    val header = "-----------------------------------Results-----------------------------------"

    val body = results.get.mkString("\n")

    Seq(
      header,
      body,
      s"Tests: $total",
      s"Passed: $total",
      s"Failed: $failure"
    ).mkString("\n")
  }
}

