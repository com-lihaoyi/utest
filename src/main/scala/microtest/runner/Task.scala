package microtest.runner

import sbt.testing.{Logger, EventHandler, TaskDef}
import microtest.util.Tree
import microtest.framework.{TestSuite, Result}
import sbt.testing
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

class Task(val taskDef: TaskDef,
           path: String,
           printer: microtest.Formatter,
           addTotal: Int => Unit,
           addResult: Tree[Result] => Unit,
           progressString: => String)
           extends sbt.testing.Task{

  val cls = Class.forName(taskDef.fullyQualifiedName() + "$")
  val tests = cls.getField("MODULE$").get(cls).asInstanceOf[TestSuite].tests
  addTotal(tests.length)

  def tags(): Array[String] = Array()

  def execute(eventHandler: EventHandler, loggers: Array[Logger]): Array[testing.Task] = {
    def doStuff(s: Seq[String]) = {
      def addSingleResult(path: Seq[String], s: Result): Unit = {
        val str = progressString + printer.formatResult(path, s)
        loggers.foreach(_.info(str))
      }

      val results = tests.run(
        addSingleResult,
        Seq(taskDef.fullyQualifiedName()),
        s
      )

      addResult(results)
    }

    val fqName = taskDef.fullyQualifiedName()

    if (fqName.startsWith(path)){
      doStuff(Nil)
    } else if (path.startsWith(fqName)){
      doStuff(path.drop(fqName.length).split("\\.").filter(_.length > 0))
    }else{
      // do nothing
    }

    Array()
  }
}
