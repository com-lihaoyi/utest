package utest
package framework

import java.util.concurrent.ExecutionException

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
   * Creates a test from a set of children, a name a a [[TestThunKTree]].
   * Generally called by the [[TestSuite.apply]] macro and doesn't need to
   * be called manually.
   */
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
 * comes contextually when traversing the [[utest.framework.TestTreeSeq]] to reach it.
 */
case class Test(name: String, TestThunkTree: TestThunkTree)

/**
 * Extension methods on `TreeSeq[Test]`
 */
class TestTreeSeq(tests: Tree[Test]) {
  /**
   * Runs this `Tree[Test]` asynchronously and returns a `Future` containing
   * the tree of the results.
   *
   * @param onComplete Called each time a single [[Test]] finishes
   * @param path The integer path of the current test in its [[TestThunkTree]]
   * @param strPath The path to the current test
   * @param outerError Whether or not an outer test failed, and this test can
   *                   be failed immediately without running
   * @param ec Used to
   */
  def runFuture(onComplete: (Seq[String], Result) => Unit,
               path: Seq[Int],
               strPath: Seq[String] = Nil,
               wrap: (=> Future[Any]) => Future[Any] ,
               outerError: Future[Option[SkippedOuterFailure]] = Future.successful(Option.empty[SkippedOuterFailure]))
              (implicit ec: concurrent.ExecutionContext): Future[Tree[Result]] = Future {
    val start = Deadline.now
    // Special-case tests which return a future, in order to wait for them to finish
    val futurized = wrap{
      val tryResult = outerError.flatMap {
        case None =>
          // try-catch manually so that fatal errors are handled (they're not by Try & Future).
          Future(
            try Future.successful(tests.value.TestThunkTree.run(path.toList))
            catch {case e: Throwable => Future.failed(e)}
          ).flatMap(identity)
        case Some(f) => throw f
      }
      tryResult flatMap{
        case f: Future[_] => f
        case f => Future.successful(f)
      }
    }
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

    val thisError = futurized.map{
      case t => None
    }.recover{
      case e: SkippedOuterFailure => Some(e)
      case e => Some(SkippedOuterFailure(strPath, unbox(e)))
    }

    def runChildren(tail: Seq[Tree[Test]], results: List[Tree[Result]], index: Int): Future[List[Tree[Result]]] = {
      tail.headOption match{
        case None => Future(results)
        case Some(head) =>
          val future = new TestTreeSeq(head).runFuture(
            onComplete,
            path :+ index,
            strPath :+ head.value.name,
            wrap,
            thisError
          )
          future.flatMap { result => runChildren(tail.tail, results :+ result, index+1) }
      }
    }

    val futureResults = runChildren(tests.children, List(), 0)

    futurized.map{
      case value => Success(value)
    }.recover{
      case e => Failure(e)
    }.flatMap(res =>
      futureResults.map { results =>
        val res1 = res match{
          case Failure(e: java.util.concurrent.ExecutionException)
            if e.getMessage == "Boxed Error" =>
            Failure(e.getCause)
          case r => r
        }
        val end = Deadline.now
        val result = Result(tests.value.name, res1, end.time.toMillis - start.time.toMillis)
        onComplete(strPath, result)
        new Tree(result, results)
      }
    )
  }.flatMap(x => x)


  def resolve(testPath: Seq[String]) = {
    val indices = collection.mutable.Buffer.empty[Int]
    var current = tests
    var strings = testPath.toList
    while(strings.nonEmpty){
      val head :: tail = strings
      strings = tail
      val index = current.children.indexWhere(_.value.name == head)
      indices.append(index)
      if (!current.children.isDefinedAt(index)){
        throw NoSuchTestException(testPath:_*)
      }
      current = current.children(index)
    }
    (indices, current)
  }

  def runAsync(onComplete: (Seq[String], Result) => Unit = (_, _) => (),
               strPath: Seq[String] = Nil,
               testPath: Seq[String] = Nil,
               wrap: (=> Future[Any]) => Future[Any] = x => x)
              (implicit ec: concurrent.ExecutionContext): Future[Tree[Result]] = {

    val (indices, current) = resolve(testPath)
    new TestTreeSeq(current).runFuture(onComplete, indices, strPath, wrap)
  }

  def run(onComplete: (Seq[String], Result) => Unit = (_, _) => (),
          strPath: Seq[String] = Nil,
          testPath: Seq[String] = Nil,
          wrap: (=> Future[Any]) => Future[Any] = x => x)
         (implicit ec: concurrent.ExecutionContext): Tree[Result] = {

    PlatformShims.await(runAsync(onComplete, strPath, testPath, wrap))
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
