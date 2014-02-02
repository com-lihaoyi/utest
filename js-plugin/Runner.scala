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
  val failure = new AtomicInteger(0)

  @tailrec final def addResult(r: String): Unit = {
    val old = results.get()
    if (!results.compareAndSet(old, r :: old)) addResult(r)
  }

  def progressString = {
    s"${success.get + failure.get}/${total.get}".padTo(8, ' ')
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
        win => {
          if (win)success.addAndGet(1)
          else failure.addAndGet(1)
        },
        total.addAndGet,
        addResult,

        progressString
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

