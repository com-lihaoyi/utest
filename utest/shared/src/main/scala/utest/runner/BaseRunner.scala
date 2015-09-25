package utest.runner
import acyclic.file
import sbt.testing._
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.annotation.tailrec

import utest.framework.ExecutionContext
import utest.framework.TestSuite

import scala.concurrent.Await
import scala.util.{Failure, Success}
import concurrent.duration._

import org.scalajs.testinterface.TestUtils
abstract class BaseRunner(val args: Array[String],
                          val remoteArgs: Array[String],
                          testClassLoader: ClassLoader)
                          extends sbt.testing.Runner{

  /**
   * Actually performs the running of a particular test
   *
   * @param selector The name of the test within the test class/object
   * @param loggers SBT loggers which are interested in the logspam generated
   * @param name The name of the test class/object
   */
  def runUTestTask(selector: Seq[String],
                   loggers: Seq[Logger],
                   name: String,
                   eventHandler: EventHandler) = {
    val suite = TestUtils.loadModule(name, testClassLoader).asInstanceOf[TestSuite]
    val selectorString = selector.mkString(".")
    def handleEvent(op: OptionalThrowable, st: Status) = {
      eventHandler.handle(new Event {
        def fullyQualifiedName() = selectorString
        def throwable() = op
        def status() = st
        def selector() = new TestSelector(selectorString)
        def fingerprint() = new SubclassFingerprint {
          def superclassName = "utest.framework.TestSuite"
          def isModule = true
          def requireNoArgConstructor = true
        }
        def duration() = 0
      })
    }
    utest.runSuite(
      suite,
      selector,
      args,
      addCount = b => if(b) incSuccess() else  incFailure(),
      log = msg => {
        handleEvent(new OptionalThrowable(), Status.Success)
        loggers.foreach(_.info(progressString + name + "" + msg))
      },
      logFailure = (msg, thrown) => {
        handleEvent(new OptionalThrowable(thrown), Status.Failure)

        // Trim the stack trace so all the utest internals don't get shown,
        // since the user probably doesn't care about those anyway
        thrown.setStackTrace(
          thrown.getStackTrace.takeWhile(_.getClassName != "utest.framework.TestThunkTree")
        )
        addFailure(progressString + name + "" + msg)
        addTrace(
          if (thrown.isInstanceOf[utest.framework.SkippedOuterFailure]) ""
          else thrown.getStackTrace.map(_.toString).mkString("\n")
        )
      },
      s => addTotal(s.toInt)
    ).map(addResult)(ExecutionContext.RunNow)
  }

  def addResult(r: String): Unit
  def addFailure(r: String): Unit
  def addTrace(trace: String): Unit
  def addTotal(v: Int): Unit
  def incSuccess(): Unit
  def incFailure(): Unit

  def successCount: Int
  def failureCount: Int
  def totalCount: Int

  def progressString = {
    s"${successCount + failureCount}/$totalCount".padTo(8, ' ')
  }

  def tasks(taskDefs: Array[TaskDef]) = taskDefs.map(makeTask)

  private def makeTask(taskDef: TaskDef): sbt.testing.Task = {
    val path = args.lift(0)
      .filter(_(0) != '-')
      .getOrElse("")

    new Task(taskDef, args, path, runUTestTask)
  }

  // Scala.js test interface specific methods
  def deserializeTask(task: String, deserializer: String => TaskDef): sbt.testing.Task =
    makeTask(deserializer(task))

  def serializeTask(task: sbt.testing.Task, serializer: TaskDef => String): String =
    serializer(task.taskDef)

}



