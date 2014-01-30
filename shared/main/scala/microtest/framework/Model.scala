package microtest.framework

import scala.util.{Random, Success, Failure, Try}
import scala.concurrent.duration.Deadline


import scala.language.experimental.macros
import scala.concurrent.{Await, Future, ExecutionContext}
import concurrent.duration._
import microtest.util.Tree
import microtest.Flattener

object Test{
  def create(tests: (String, (String, TestThunkTree) => Tree[Test])*)
            (name: String, testTree: TestThunkTree): Tree[Test] = {
    new Tree(
      new Test(name, testTree),
      tests.map{ case (k, v) => v(k, testTree) }
    )
  }
}

/**
 * Represents the metadata around a single test in a [[TestTreeSeq]]. This is
 * a pretty simple data structure, as much of the information related to it
 * comes contextually when traversing the [[microtest.framework.TestTreeSeq]] to reach it.
 */
case class Test(name: String, TestThunkTree: TestThunkTree)

/**
 * Extension methods related to `TreeSeq[Test]`
 */
class TestTreeSeq(tests: Tree[Test]) {
  def runAsync(onComplete: (Seq[String], Result) => Unit,
               path: Seq[Int],
               strPath: Seq[String] = Nil,
               outerError: Option[SkippedDueToOuterFailureError] = None)
              (implicit ec: ExecutionContext): Future[Tree[Result]] = {
    Flattener.flatten(Future{
      val start = Deadline.now
      val tryResult =
        outerError.fold(Try(tests.value.TestThunkTree.run(path.toList)))(Failure(_))

      val thisError = tryResult match{
        case Success(_) => None
        case Failure(e: SkippedDueToOuterFailureError) => Some(e)
        case Failure(e) => Some(SkippedDueToOuterFailureError(strPath, e))
      }

      val childRuns =
        tests.children
          .zipWithIndex.map{ case (v, i) =>
          v.runAsync(onComplete, path :+ i, strPath :+ v.value.name, thisError)
        }

      val temp = childRuns.foldLeft(Future(List.empty[Tree[Result]])){
        case (a, b) => Flattener.flatten(a.map(a => b.map(b => a :+ b)))
      }

      val sequenced = temp.map{ results =>
        val end = Deadline.now
        val result = Result(tests.value.name, tryResult, start, end)
        onComplete(strPath, result)
        new Tree(
          result,
          results
        )
      }

      sequenced
    })
  }

  def run(onComplete: (Seq[String], Result) => Unit = (_, _) => (),
          strPath: Seq[String] = Nil,
          testPath: Seq[String] = Nil)
         (implicit ec: ExecutionContext): Tree[Result] = {
    val indices = collection.mutable.Buffer.empty[Int]
    var current = tests
    var strings = testPath.toList
    println("Resolving... " + strPath.mkString("."))
    while(!strings.isEmpty){
      val head :: tail = strings
      strings = tail
      val index = current.children.indexWhere(_.value.name == head)
      indices.append(index)
      current = current.children.toList(index)
    }
    println("Running Async... " + strPath.mkString("."))
    val future = current.runAsync(onComplete, indices, strPath)
    println("Result... " + future.value)

    Flattener.await(future)
  }
}

object TestThunkTree{
  def create(inner: => (Any, Seq[TestThunkTree])) = new TestThunkTree(inner)
}

/**
 * A tree of nested lexical scopes that accompanies the tree of tests. This
 * is separated from the tree of metadata in [[TestTreeSeq]] in order to allow
 * you to query the metadata without executing the tests..
 */
class TestThunkTree(inner: => (Any, Seq[TestThunkTree])){
  def run(path: List[Int]): Any = {
    path match {
      case head :: tail =>
        val (res, children) = inner
        children(head).run(tail)
      case Nil =>
        val (res, children) = inner
        res
    }
  }
}

/**
 * A single test's result after execution. Any exception thrown or value
 * returned by the test is stored in `value`. The value returned can be used
 * in another test, which adds a dependency between them.
 */
case class Result(name: String,
                  value: Try[Any],
                  startTime: Deadline,
                  endTime: Deadline) {

  def duration = endTime - startTime
}
