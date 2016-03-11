package utest.runner
import acyclic.file
import sbt.testing._
import utest.TestSuite

import scala.util.Failure

import org.scalajs.testinterface.TestUtils
abstract class BaseRunner(val args: Array[String],
                          val remoteArgs: Array[String],
                          testClassLoader: ClassLoader)
                          extends sbt.testing.Runner{
  def addResult(r: String): Unit
  def addFailure(r: String): Unit
  def addTrace(trace: String): Unit
  def addTotal(v: Int): Unit
  def incSuccess(): Unit
  def incFailure(): Unit

  def tasks(taskDefs: Array[TaskDef]) = taskDefs.map(makeTask)

  /**
    * Actually performs the running of a particular test
    *
    * @param selector The name of the test within the test class/object
    * @param loggers SBT loggers which are interested in the logspam generated
    * @param name The name of the test class/object
    */
  def runSuite(selector: Seq[String],
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
          def superclassName = "utest.TestSuite"
          def isModule = true
          def requireNoArgConstructor = true
        }
        def duration() = 0
      })
    }

    val title = s"Starting Suite " + name
    val dashes = "-" * ((80 - title.length) / 2)
    loggers.foreach(_.info(dashes + title + dashes))

    val (indices, found) = suite.tests.resolve(selector)

    addTotal(found.length)

    implicit val ec =
      if (utest.framework.ArgParse.find("--parallel", _.toBoolean, false, true)(args)){
        scala.concurrent.ExecutionContext.global
      }else{
        utest.framework.ExecutionContext.RunNow
      }

    val results = suite.tests.runAsync(
      (subpath, s) => {
        if(s.value.isSuccess) incSuccess() else  incFailure()

        val str = suite.formatSingle(selector ++ subpath, s)
        handleEvent(new OptionalThrowable(), Status.Success)
        str.foreach{msg => loggers.foreach(_.info(name + "" + msg))}
        s.value match{
          case Failure(e) =>
            handleEvent(new OptionalThrowable(e), Status.Failure)
            // Trim the stack trace so all the utest internals don't get shown,
            // since the user probably doesn't care about those anyway
            e.setStackTrace(
              e.getStackTrace.takeWhile(_.getClassName != "utest.framework.TestThunkTree")
            )
            addFailure(name + "" + str.getOrElse(""))
            addTrace(
              if (e.isInstanceOf[utest.framework.SkippedOuterFailure]) ""
              else e.getStackTrace.map(_.toString).mkString("\n")
            )
          case _ => ()
        }
      },
      testPath = selector,
      wrap = suite.utestWrap(_)(ec)
    )(ec)

    results.map(suite.format).map(_.foreach(addResult))
  }


  private def makeTask(taskDef: TaskDef): sbt.testing.Task = {
    val path = args.lift(0).filter(_(0) != '-').getOrElse("")
    new Task(taskDef, args, path, runSuite)
  }
  // Scala.js test interface specific methods
  def deserializeTask(task: String, deserializer: String => TaskDef): sbt.testing.Task =
    makeTask(deserializer(task))

  def serializeTask(task: sbt.testing.Task, serializer: TaskDef => String): String =
    serializer(task.taskDef)

}
