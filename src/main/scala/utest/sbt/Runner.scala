package utest.sbt
import utest.toTestSeq
import sbt.testing._
import sbt.testing
import collection.mutable
import utest.framework.{TestTreeSeq, TestSuite, Result}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.concurrent.ExecutionContext.Implicits.global
import utest.util.Tree
import scala.annotation.tailrec

class Runner(val args: Array[String],
             val remoteArgs: Array[String],
             val printer: utest.Formatter)
             extends sbt.testing.Runner{


  val results = new AtomicReference(List.empty[Tree[Result]])

  var total = 0

  var completeCounter = new AtomicInteger(0)

  def progressString = {
    s"${completeCounter.incrementAndGet()}/$total".padTo(8, ' ')
  }
  def completed = results.get.flatMap(_.toSeq)

  def tasks(taskDefs: Array[TaskDef]): Array[sbt.testing.Task] = {

    val path = args.lift(0)
                   .filter(_(0) != '-')
                   .getOrElse("")

    for(taskDef <- taskDefs) yield {
      val cls = Class.forName(taskDef.fullyQualifiedName() + "$")
      val tests = cls.getField("MODULE$").get(cls).asInstanceOf[TestSuite].tests
      total += tests.length
      new Task(taskDef, tests, path, printer, addResult, progressString)
    }
  }

  @tailrec final def addResult(r: Tree[Result]): Unit = {
    val old = results.get()
    if (!results.compareAndSet(old, r :: old)) addResult(r)
  }

  def done(): String = {
    val header = "-----------------------------------Results-----------------------------------"
    val body = results.get
                      .map(printer.format)
                      .mkString("\n")

    Seq(
      header,
      body,
      s"Tests: ${completed.length}",
      s"Passed: ${completed.count(_.value.isSuccess)}",
      s"Failed: ${completed.count(_.value.isFailure)}"
    ).mkString("\n")
  }
}

class Task(val taskDef: TaskDef,
           tests: utest.util.Tree[utest.framework.Test],
           path: String,
           printer: utest.Formatter,
           addResult: Tree[Result] => Unit,
           progressString: => String)
           extends sbt.testing.Task{


  def tags(): Array[String] = Array()

  def execute(eventHandler: EventHandler, loggers: Array[Logger]): Array[testing.Task] = {
    def doStuff(s: Seq[String]) = {
      def addSingleResult(path: Seq[String], s: Result): Unit = {
        val str = progressString + printer.formatResult(path, s)
        loggers.map(_.info(str))
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
