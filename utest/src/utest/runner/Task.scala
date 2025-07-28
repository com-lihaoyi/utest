package utest
package runner
//import acyclic.file
import sbt.testing.*
import sbt.testing

import scala.concurrent.{Await, Future}
import concurrent.duration.*
import utest.framework.{ExecutionContext, PlatformShims, Tree}

class Task(_taskDef: TaskDef,
           runUTestTask: (Seq[Logger], EventHandler) => Future[Unit])
           extends sbt.testing.Task{

  def taskDef(): TaskDef = _taskDef

  def tags(): Array[String] = Array()

  def execute(eventHandler: EventHandler, loggers: Array[Logger]): Array[testing.Task] = {
    PlatformShims.await(runUTestTask(loggers, eventHandler))
    Array()
  }

  def execute(eventHandler: EventHandler,
              loggers: Array[Logger],
              continuation: Array[testing.Task] => Unit) = {

    implicit val ec = ExecutionContext.RunNow

    runUTestTask(loggers, eventHandler).recover { case t =>
      loggers.foreach(_.trace(t))
    }.onComplete{_ =>
      continuation(Array())
    }
  }
}
