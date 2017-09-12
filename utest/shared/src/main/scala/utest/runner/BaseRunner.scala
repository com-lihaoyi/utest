package utest
package runner
//import acyclic.file
import sbt.testing._

import scala.util.Failure
import org.scalajs.testinterface.TestUtils
import utest.framework.Tree
object BaseRunner{
  /**
    * Checks whether the given query needs the TestSuite at testSuitePath
    * to execute or not. It needs to execute if one of the terminal nodes in the
    * query is one of the TestSuite's ancestors in the tree, or one-or-more of
    * it's children, but not if the terminal nodes are unrelated.
    */
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
                          testClassLoader: ClassLoader,
                          useSbtLoggers: Boolean)
                          extends sbt.testing.Runner{

  lazy val path = args.headOption.filter(_(0) != '-')
  lazy val query = path
    .map(Query(_))
    .getOrElse(Nil)

  def addResult(r: String): Unit
  def addFailure(r: String): Unit
  def addTrace(trace: String): Unit
  def incSuccess(): Unit
  def incFailure(): Unit

  def tasks(taskDefs: Array[TaskDef]) = {
    val allPaths = query.flatMap(_.leafPaths)
    // This is in theory quadratic but it is probably find
    val unknownPaths = allPaths.filter( p =>
      !taskDefs.exists{ t =>
        val taskSegments = t.fullyQualifiedName().split('.')
        taskSegments.startsWith(p) || p.startsWith(taskSegments)
      }
    )
    if (unknownPaths.nonEmpty){
      throw new NoSuchTestException(unknownPaths:_*)
    }else for{
      taskDef <- taskDefs
      if BaseRunner.checkOverlap(query, taskDef.fullyQualifiedName().split('.'))
    } yield makeTask(taskDef)
  }

  def runSuite(loggers: Seq[Logger],
               suiteName: String,
               eventHandler: EventHandler,
               taskDef: TaskDef) = {
    val suite = TestUtils.loadModule(suiteName, testClassLoader).asInstanceOf[TestSuite]

    def handleEvent(op: OptionalThrowable,
                    st: Status,
                    subpath: Seq[String],
                    millis: Long) = {

      eventHandler.handle(new Event {
        def fullyQualifiedName() = suiteName
        def throwable() = op
        def status() = st
        def selector() = {
          new NestedTestSelector(suiteName, subpath.mkString("."))
        }
        def fingerprint() = taskDef.fingerprint()
        def duration() = millis
      })
    }

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
      rec(query, suiteName.split('.').toList)
    }

    implicit val ec = utest.framework.ExecutionContext.RunNow


    val results = TestRunner.runAsync(
      suite.tests,
      (subpath, result) => {
        val str = suite.formatSingle(suiteName.split('.') ++ subpath, result)

        str.foreach{msg =>
          if (useSbtLoggers) loggers.foreach(_.info(msg.split('\n').mkString("\n")))
          else println(msg)
        }
        result.value match{
          case Failure(e) =>
            handleEvent(new OptionalThrowable(e), Status.Failure, subpath, result.milliDuration)
            // Trim the stack trace so all the utest internals don't get shown,
            // since the user probably doesn't care about those anyway
            e.setStackTrace(
              e.getStackTrace.takeWhile(_.getClassName != "utest.framework.TestThunkTree")
            )
            incFailure()
            addFailure(str.fold("")(_.render))
            addTrace(e.getStackTrace.map(_.toString).mkString("\n"))
          case _ =>
            handleEvent(new OptionalThrowable(), Status.Success, subpath, result.milliDuration)
            incSuccess()
        }
      },
      query = innerQuery,
      executor = suite,
      ec = ec
    )

    results.map(suite.formatSummary(suiteName, _).foreach(x => addResult(x.render)))
  }


  private def makeTask(taskDef: TaskDef): sbt.testing.Task = {
    new Task(taskDef, runSuite(_, taskDef.fullyQualifiedName(), _, taskDef))
  }
  // Scala.js test interface specific methods
  def deserializeTask(task: String, deserializer: String => TaskDef): sbt.testing.Task =
    makeTask(deserializer(task))

  def serializeTask(task: sbt.testing.Task, serializer: TaskDef => String): String =
    serializer(task.taskDef)

}
