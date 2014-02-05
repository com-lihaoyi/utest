package utest.runner

import sbt.testing.{Logger, EventHandler, TaskDef}
import sbt.testing
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

class Task(val taskDef: TaskDef,
           args: Array[String],
           path: String,
           doStuff: (Seq[String], Seq[Logger], String) => Unit)
           extends sbt.testing.Task{

  def tags(): Array[String] = Array()

  def execute(eventHandler: EventHandler, loggers: Array[Logger]): Array[testing.Task] = {

    val fqName = taskDef.fullyQualifiedName()
    if (fqName.startsWith(path)){
      doStuff(Nil, loggers, fqName)
    } else if (path.startsWith(fqName)){
      doStuff(path.drop(fqName.length).split("\\.").filter(_.length > 0), loggers, fqName)
    }else{
      // do nothing
    }

    Array()
  }
}
