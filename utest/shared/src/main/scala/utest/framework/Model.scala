package utest
package framework

import utest.Query

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
  def runAsync(onComplete: (Seq[String], Result) => Unit = (_, _) => (),
               query: Query#Trees = Nil,
               wrap: (=> Future[Any]) => Future[Any] = x => x)
              (implicit ec: concurrent.ExecutionContext): Future[Tree[Result]] = {
    val thunkTree = recQuery(tests, resolve(tests, query), Nil)

    val forced = thunkTree.map{case (name, thunk) => () =>
      val start = Deadline.now
      val res: Future[Any] = wrap{
        try thunk() match{
          case x: Future[_] => x
          case notFuture => Future.successful(notFuture)
        } catch{
          case e: Throwable => Future.failed(e)
        }
      }

      def millis = (Deadline.now-start).toMillis
      res.map(v => Result(name, Success(v), millis))
         .recover{case e: Throwable => Result(name, Failure(unbox(e)), millis)}
    }

    recFutures(forced)
  }

  def recFutures[T](t: Tree[() => Future[T]])
                   (implicit ec: concurrent.ExecutionContext): Future[Tree[T]] = {
    for{
      v <- t.value()
      childValues <- Future.traverse(t.children.toSeq)(recFutures(_))
    } yield Tree(v, childValues:_*)
  }

  def run(onComplete: (Seq[String], Result) => Unit = (_, _) => (),
          query: Seq[Tree[String]] = Nil,
          wrap: (=> Future[Any]) => Future[Any] = x => x)
         (implicit ec: concurrent.ExecutionContext): Tree[Result] = {

    PlatformShims.await(runAsync(onComplete, query, wrap))
  }

  def resolve(test: Tree[Test], query: Seq[Tree[String]]): Seq[Tree[Int]] = {
    val strToIndex = test.children.map(_.value.name).zipWithIndex.toMap
    for(q <- query) yield {
      val index = strToIndex(q.value)
      Tree(index, resolve(test.children(index), q.children):_*)
    }
  }

  def recQuery(test: Tree[Test],
               query: Seq[Tree[Int]],
               revIntPath: List[Int]): Tree[(String, () => Any)] = {

    if (query.isEmpty) recTests(test, revIntPath)
    else{
      val children = for(subquery <- query) yield recQuery(
        test.children(subquery.value),
        subquery.children,
        subquery.value :: revIntPath
      )

      Tree((test.value.name, () => ()), children:_*)
    }
  }

  def recTests(test: Tree[Test], revIntPath: List[Int]): Tree[(String, () => Any)] = {
    if (test.children.isEmpty) {
      Tree((test.value.name, () => test.value.thunkTree.run(revIntPath.reverse)))
    }
    else{
      Tree(
        (test.value.name, () => ()),
        test.children.zipWithIndex.map{case (c, i) => recTests(c, i :: revIntPath)}:_*
      )
    }
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
