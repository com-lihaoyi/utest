package utest
package runner
//import acyclic.file
import sbt.testing._
import utest.TestSuite

import scala.util.Failure
import org.scalajs.testinterface.TestUtils
import utest.framework.{Test, Tree}
object BaseRunner{
  def checkOverlap(query: Seq[Tree[String]], testSuitePath: Seq[String]): Boolean = {
    def rec(current: Seq[Tree[String]], remainingSegments: List[String]): Boolean = {
      (current, remainingSegments) match {
        // The query refers to a *parent* node of this TestSuite, thus this tree gets run
        case (Nil, _) => true
        // The query refers to a *child* node of this TestSuite, also meaning this tree gets run
        case (_, Nil) => true

        case (subtrees, nextSegment :: rest) =>
          subtrees.find(_.value == nextSegment) match{
            // This query refers only to sibling nodes for this TestSuite, so we do not run it
            case None => false
            case Some(subtree) => rec(subtree.children, rest)
          }
      }
    }
    rec(query, testSuitePath.toList)
  }

}
abstract class BaseRunner(val args: Array[String],
                          val remoteArgs: Array[String],
                          testClassLoader: ClassLoader)
                          extends sbt.testing.Runner{
  val path = args.lift(0).filter(_(0) != '-')
  val query = path.map(Query.parse(_).right.get).getOrElse(Nil)

  def addResult(r: String): Unit
  def addFailure(r: String): Unit
  def addTrace(trace: String): Unit
  def addTotal(v: Int): Unit
  def incSuccess(): Unit
  def incFailure(): Unit



  def tasks(taskDefs: Array[TaskDef]) = {
    for{
      taskDef <- taskDefs
      if BaseRunner.checkOverlap(query, taskDef.fullyQualifiedName().split('.'))
    } yield makeTask(taskDef)
  }

  def runSuite(loggers: Seq[Logger],
               name: String,
               eventHandler: EventHandler) = {
    val suite = TestUtils.loadModule(name, testClassLoader).asInstanceOf[TestSuite]

    def handleEvent(op: OptionalThrowable, st: Status) = {
      eventHandler.handle(new Event {
        def fullyQualifiedName() = path.getOrElse("")
        def throwable() = op
        def status() = st
        def selector() = new TestSelector(path.getOrElse(""))
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



    val innerQuery = {
      def rec(currentQuery: Query#Trees, segments: List[String]): Query#Trees = {
        segments match{
          case head :: tail =>
            currentQuery.find(_.value == head) match{
              case None => Nil
              case Some(sub) => rec(sub.children, tail)
            }
          case Nil => currentQuery
        }

      }
      rec(query, name.split('.').toList)
    }

    implicit val ec = utest.framework.ExecutionContext.RunNow


    val results = suite.tests.runFuture(
      (subpath, s) => {
        if(s.value.isSuccess) incSuccess() else incFailure()

        val str = suite.formatSingle(name.split('.') ++ subpath, s)
        handleEvent(new OptionalThrowable(), Status.Success)
        str.foreach{msg => loggers.foreach(_.info(msg.split('\n').mkString("\n")))}
        s.value match{
          case Failure(e) =>
            handleEvent(new OptionalThrowable(e), Status.Failure)
            // Trim the stack trace so all the utest internals don't get shown,
            // since the user probably doesn't care about those anyway
            e.setStackTrace(
              e.getStackTrace.takeWhile(_.getClassName != "utest.framework.TestThunkTree")
            )
            addFailure(str.fold("")(_.render))
            addTrace(
              if (e.isInstanceOf[SkippedOuterFailure]) ""
              else e.getStackTrace.map(_.toString).mkString("\n")
            )
          case _ => ()
        }
      },
      query = innerQuery,
      wrap = suite.utestWrap(_)(ec)
    )(ec)

    results.map(suite.format).map(_.foreach(x => addResult(x.render)))
  }


  private def makeTask(taskDef: TaskDef): sbt.testing.Task = {
    new Task(taskDef, runSuite(_, taskDef.fullyQualifiedName(), _))
  }
  // Scala.js test interface specific methods
  def deserializeTask(task: String, deserializer: String => TaskDef): sbt.testing.Task =
    makeTask(deserializer(task))

  def serializeTask(task: sbt.testing.Task, serializer: TaskDef => String): String =
    serializer(task.taskDef)

}
