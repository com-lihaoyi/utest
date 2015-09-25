package utest.runner
import acyclic.file
import sbt.testing._
import sbt.testing
import scala.concurrent.{Future, Await}
import concurrent.duration._

import utest.framework.ExecutionContext

class Task(val taskDef: TaskDef,
           args: Array[String],
           path: String,
           runUTestTask: (Seq[String], Seq[Logger], String, EventHandler) => Future[Unit])
           extends sbt.testing.Task{

  def tags(): Array[String] = Array()

  def execute(eventHandler: EventHandler, loggers: Array[Logger]): Array[testing.Task] = {
    Await.result(executeInternal(eventHandler, loggers), Duration.Inf)
    Array()
  }

  def execute(eventHandler: EventHandler,
              loggers: Array[Logger],
              continuation: Array[testing.Task] => Unit) = {

    implicit val ec = ExecutionContext.RunNow

    val logged = executeInternal(eventHandler, loggers).recover { case t =>
      loggers.foreach(_.trace(t))
    }.onComplete{_ =>
      continuation(Array())
    }
  }

  private def executeInternal(eventHandler: EventHandler, loggers: Array[Logger]) = {

    val allPaths: Seq[String] =
      if (!path.endsWith("}")) Seq(path)
      else{
        val (first, last) = path.splitAt(path.lastIndexOf("{"))
        last.drop(1).dropRight(1).split(",").map(first + _.trim())
      }

    val fqName = taskDef.fullyQualifiedName()

    if (allPaths.exists(fqName.startsWith)){
      runUTestTask(Nil, loggers, fqName, eventHandler)
    } else {
      implicit val ex = ExecutionContext.RunNow
      val futs = allPaths.filter(_.startsWith(fqName)).map{p =>
        runUTestTask(
          p.drop(fqName.length).split("\\.").filter(_.length > 0),
          loggers,
          fqName,
          eventHandler
        )
      }
      Future.sequence(futs)
    } // else do nothing
  }
}
