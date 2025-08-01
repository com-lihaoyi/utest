package utest
package runner
//import acyclic.file
import sbt.testing._

import scala.concurrent.Future
import scala.util.Failure
import utest.framework.{PlatformShims, StackMarker, Tree}
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
                          _remoteArgs: Array[String],
                          testClassLoader: ClassLoader,
                          useSbtLoggers: Boolean,
                          formatter: utest.framework.Formatter,
                          startHeader: Option[String => String] = None)
                          extends sbt.testing.Runner{

  def remoteArgs(): Array[String] = _remoteArgs

  def registerSuite(x: TestSuite): Unit = ()
  lazy val path = args.headOption.filter(_(0) != '-')
  lazy val query = path
    .map(TestQueryParser(_))
    .getOrElse(Nil)

  def addResult(r: String): Unit
  def addFailure(r: String): Unit
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
      fromSelectors = pathsFromSelectors(taskDef)
      fullQuery = TestQueryParser.collapse(query ++ fromSelectors)
      if BaseRunner.checkOverlap(fullQuery, taskDef.fullyQualifiedName().split('.'))
    } yield makeTask(taskDef, fullQuery)
  }

  private def pathsFromSelectors(taskDef: TaskDef): TestQueryParser#Trees = {
    if (taskDef.selectors().exists(!_.isInstanceOf[NestedTestSelector])) Nil
    else {
      val testNames = taskDef.selectors().collect {
        case nts: NestedTestSelector => s"${nts.suiteId()}.${nts.testName()}"
      }
      try TestQueryParser(testNames.mkString(","))
      catch { case _: Throwable => Nil }
    }
  }

  def runSuite(loggers: Seq[Logger],
               suiteName: String,
               eventHandler: EventHandler,
               taskDef: TaskDef,
               fullQuery: TestQueryParser#Trees): Future[Option[TestSuite]] = {

    startHeader.foreach(h => println(h(path.fold("")(" " + _))))

    def handleEvent(op: OptionalThrowable,
                    st: Status,
                    subpath: Seq[String],
                    millis: Long) = {

      eventHandler.synchronized {
        eventHandler.handle(new Event {
          def fullyQualifiedName() = suiteName
          def throwable() = op
          def status() = st
          def selector() = {
            new NestedTestSelector(suiteName, subpath.mkString("."))
          }
          def fingerprint() = taskDef.fingerprint()
          def duration() = millis

          override def toString: String = selector().toString
        })
      }
    }

    val suiteEither =
      try {
        Right(
          StackMarker.dropOutside(
            PlatformShims.loadModule(suiteName, testClassLoader).asInstanceOf[TestSuite]
          )
        )
      } catch{case e: Throwable => Left(e)}

    def log(msg: String) = {
      if (useSbtLoggers) loggers.foreach(_.info(msg.split('\n').mkString("\n")))
      else println(msg)
    }
    suiteEither match{
      case Left(e) =>
        val dummyDuration = 0
        handleEvent(new OptionalThrowable(e), Status.Failure, Nil, dummyDuration)
        val result = utest.framework.Result("lols", Failure(e), dummyDuration)
        for(fstr <- formatter.formatSingle(suiteName.split('.'), result)){

          addResult(fstr.render)
          log(fstr.render)
        }
        scala.concurrent.Future.successful(None)
      case Right(suite) =>
        registerSuite(suite)
        val innerQuery = {
          def rec(currentQuery: TestQueryParser#Trees, segments: List[String]): TestQueryParser#Trees = {
            segments match{
              case head :: tail =>
                currentQuery.find(_.value == head) match{
                  case None => Nil
                  case Some(sub) => rec(sub.children, tail)
                }
              case Nil => currentQuery
            }

          }
          rec(fullQuery, suiteName.split('.').toList)
        }

        implicit val ec = utest.framework.ExecutionContext.RunNow

        val suiteFormatter = Option(suite.utestFormatter).getOrElse(formatter)
        val results = TestRunner.runAsync(
          suite.tests,
          (subpath, result) => {
            val str = suiteFormatter.formatSingle(suiteName.split('.') ++ subpath, result)

            str.foreach(s => log(s.render))

            result.value match{
              case Failure(e) =>
                handleEvent(new OptionalThrowable(e), Status.Failure, subpath, result.milliDuration)
                incFailure()
                addFailure(str.fold("")(_.render))
              case _ =>
                handleEvent(new OptionalThrowable(), Status.Success, subpath, result.milliDuration)
                incSuccess()
            }
          },
          query = innerQuery,
          executor = suite,
          ec = ec
        )

        results.map { r =>
          suiteFormatter.formatSummary(suiteName, r).foreach(x => addResult(x.render))
          Some(suite)
        }
    }
  }


  private def makeTask(taskDef: TaskDef, fullQuery: TestQueryParser#Trees): sbt.testing.Task = {
    new runner.Task(taskDef, runSuite(_, taskDef.fullyQualifiedName(), _, taskDef, fullQuery))
  }
  // Scala.js test interface specific methods
  def deserializeTask(task: String, deserializer: String => TaskDef): sbt.testing.Task = {
    val taskDef = deserializer(task)
    val fromSelectors = pathsFromSelectors(taskDef)
    val fullQuery = TestQueryParser.collapse(query ++ fromSelectors)
    makeTask(taskDef, fullQuery)
  }

  def serializeTask(task: sbt.testing.Task, serializer: TaskDef => String): String =
    serializer(task.taskDef())

}
