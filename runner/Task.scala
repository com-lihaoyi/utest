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
    val allPaths: Seq[String] =
      if (!path.endsWith("}")) Seq(path)
      else{
         val (first, last) = path.splitAt(path.lastIndexOf("{"))
        println(first + " :: " + last)
        last.drop(1).dropRight(1).split(",").map(first + _.trim())
      }
    val fqName = taskDef.fullyQualifiedName()

    if (allPaths.exists(fqName.startsWith)){
      doStuff(Nil, loggers, fqName)
    } else allPaths.filter(_.startsWith(fqName)) foreach{p =>
        doStuff(p.drop(fqName.length).split("\\.").filter(_.length > 0), loggers, fqName)
    } // else do nothing

    Array()
  }
}
