import sbt.testing._
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.annotation.tailrec
import scala.scalajs.sbtplugin.ScalaJSEnvironment

class Runner(val args: Array[String],
             val remoteArgs: Array[String],
             environment: ScalaJSEnvironment)
             extends sbt.testing.Runner{

  println("new Runner")
  def tasks(taskDefs: Array[TaskDef]): Array[sbt.testing.Task] = {
    println("Runner.taskDefs")
    taskDefs.foreach(println)
    val path = args.lift(0)
                   .filter(_(0) != '-')
                   .getOrElse("")

    for(taskDef <- taskDefs) yield {
      new Task(
        taskDef,
        path,
        environment
      )
    }
  }

  def done(): String = {
    println("Runner.done")
    val header = "-----------------------------------Results-----------------------------------"
    Seq(
      header
    ).mkString("\n")
  }
}

