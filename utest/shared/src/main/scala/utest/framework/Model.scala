package utest
package framework

import java.util.concurrent.ExecutionException

import utest.runner.Query

import scala.collection.mutable

//import acyclic.file

import scala.util.{Success, Failure, Try}
import scala.concurrent.duration.Deadline
import scala.language.experimental.macros
import scala.concurrent.Future

import utest.PlatformShims

case class TestPath(value: Seq[String])
object TestPath{
  @reflect.internal.annotations.compileTimeOnly(
    "TestPath is only available within a uTest suite, and not outside."
  )
  implicit def synthetic: TestPath = ???
}
object Test{
  /**
   * Creates a test from a set of children, a name a a [[TestThunkTree]].
   * Generally called by the [[TestSuite.apply]] macro and doesn't need to
   * be called manually.
   */
  def create(tests: (String, (String, TestThunkTree) => Tree[Test])*)
            (name: String, testTree: TestThunkTree): Tree[Test] = {
    Tree(
      new Test(name, testTree),
      tests.map{ case (k, v) => v(k, testTree) }:_*
    )
  }
}

/**
 * Represents the metadata around a single test in a [[TestTreeSeq]]. This is
 * a pretty simple data structure, as much of the information related to it
 * comes contextually when traversing the [[utest.framework.TestTreeSeq]] to reach it.
 */
case class Test(name: String, thunkTree: TestThunkTree)

/**
 * Extension methods on `TreeSeq[Test]`
 */
class TestTreeSeq(tests: Tree[Test]) {
  /**
    * For some reason Scala futures boxes `Error`s into `ExecutionException`s,
    * so un-box them to show the user since he probably doesn't care about
    * this boxing
    */
  def unbox(res: Throwable) = res match{
    case e: java.util.concurrent.ExecutionException
      if e.getMessage == "Boxed Error" =>
      e.getCause
    case r => r
  }

  /**
   * Runs this `Tree[Test]` asynchronously and returns a `Future` containing
   * the tree of the results.
   *
   * @param onComplete Called each time a single [[Test]] finishes
   * @param outerError Whether or not an outer test failed, and this test can
   *                   be failed immediately without running
   * @param ec Used to
   */
  def runFuture(onComplete: (Seq[String], Result) => Unit,
                query: Query#Trees,
                wrap: (=> Future[Any]) => Future[Any],
                outerError: Future[Option[SkippedOuterFailure]] = Future.successful(Option.empty[SkippedOuterFailure]))
               (implicit ec: concurrent.ExecutionContext): Future[Tree[Result]] = {

    def recQuery(test: Tree[Test],
                 query: Seq[Tree[String]],
                 revIntPath: List[Int]): Tree[Result] = {

      if (query.isEmpty) recTests(test, revIntPath)
      else{
        val children = for(subquery <- query) yield{
          val index = test.children.indexWhere(_.value.name == subquery.value)
          val subtest = test.children(index)
          recQuery(subtest, subquery.children, index :: revIntPath)
        }
        Tree(
          Result(test.value.name, Success(()), 0),
          children:_*
        )

      }
    }

    def recTests(test: Tree[Test], revIntPath: List[Int]): Tree[Result] = {
      if (test.children.isEmpty) {
        val start = Deadline.now
        val res = try Success(test.value.thunkTree.run(revIntPath.reverse))
        catch{case e: Throwable => Failure(e)}

        val end = Deadline.now
        Tree(Result(test.value.name, res, (end - start).toMillis))
      }
      else{
        Tree(
          Result(test.value.name, Success(()), 0),
          test.children.zipWithIndex.map{case (c, i) => recTests(c, i :: revIntPath)}:_*
        )
      }
    }

    Future(recQuery(tests, query, Nil))
  }

  def run(onComplete: (Seq[String], Result) => Unit = (_, _) => (),
          query: Seq[Tree[String]],
          wrap: (=> Future[Any]) => Future[Any] = x => x)
         (implicit ec: concurrent.ExecutionContext): Tree[Result] = {

    PlatformShims.await(runFuture(onComplete, query, wrap))
  }
}

/**
 * A tree of nested lexical scopes that accompanies the tree of tests. This
 * is separated from the tree of metadata in [[TestTreeSeq]] in order to allow
 * you to query the metadata without executing the tests. Generally created by
 * the [[TestSuite]] macro and not instantiated manually.
 */
class TestThunkTree(inner: => (Any, Seq[TestThunkTree])){
  /**
   * Runs the test in this [[TestThunkTree]] at the specified `path`. Called
   * by the [[TestTreeSeq.run]] method and usually not called manually.
   */
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
                  milliDuration: Long)
