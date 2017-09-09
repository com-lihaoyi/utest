package utest.framework

import utest.Query

import scala.concurrent.Future
import scala.concurrent.duration.Deadline
import scala.util.{Failure, Success}
import utest.PlatformShims
trait Executor{
  import Executor._
  /**
    * Runs this `Tree[Test]` asynchronously and returns a `Future` containing
    * the tree of the results.
    *
    * @param onComplete Called each time a single [[Test]] finishes
    * @param outerError Whether or not an outer test failed, and this test can
    *                   be failed immediately without running
    * @param ec Used to
    */
  def runAsync(tests: Tree[Test],
               onComplete: (Seq[String], Result) => Unit = (_, _) => (),
               query: Query#Trees = Nil,
               wrap: (Seq[String], => Future[Any]) => Future[Any] = (_, x) => x)
              (implicit ec: concurrent.ExecutionContext): Future[Tree[Result]] = {

    val thunkTree = recQuery(tests, resolve(tests, query), Nil, Nil)

    val forced = thunkTree.map{case (terminal, revStringPath, thunk) => () =>
      if (!terminal) Future.successful(Result(revStringPath.head, Success(()), 0))
      else {

        val start = Deadline.now
        val res: Future[Any] = wrap(revStringPath.reverse,
          try thunk() match{
            case x: Future[_] => x
            case notFuture => Future.successful(notFuture)
          } catch{
            case e: Throwable => Future.failed(e)
          }
        )

        def millis = (Deadline.now-start).toMillis
        res.map(v => Result(revStringPath.head, Success(v), millis))
          .recover{case e: Throwable => Result(revStringPath.head, Failure(unbox(e)), millis)}
          .map{r => if (terminal) onComplete(revStringPath.reverse, r); r}
      }
    }

    recFutures(forced)
  }

  def run(tests: Tree[Test],
          onComplete: (Seq[String], Result) => Unit = (_, _) => (),
          query: Seq[Tree[String]] = Nil,
          wrap: (Seq[String], => Future[Any]) => Future[Any] = (_, x) => x)
         (implicit ec: concurrent.ExecutionContext): Tree[Result] = {

    PlatformShims.await(runAsync(tests, onComplete, query, wrap))
  }

}
/**
  * Created by lihaoyi on 9/9/17.
  */
object Executor extends Executor{

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


  def recFutures[T](t: Tree[() => Future[T]])
                   (implicit ec: concurrent.ExecutionContext): Future[Tree[T]] = {
    for{
      v <- t.value()
      childValues <- Future.traverse(t.children.toSeq)(recFutures(_))
    } yield Tree(v, childValues:_*)
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
               revIntPath: List[Int],
               revStringPath: List[String]): Tree[(Boolean, List[String], () => Any)] = {

    if (query.isEmpty) recTests(test, revIntPath, revStringPath)
    else{
      val children = for(subquery <- query) yield recQuery(
        test.children(subquery.value),
        subquery.children,
        subquery.value :: revIntPath,
        test.value.name :: revStringPath
      )

      Tree((false, test.value.name :: revStringPath, () => ()), children:_*)
    }
  }

  def recTests(test: Tree[Test],
               revIntPath: List[Int],
               revStringPath: List[String]): Tree[(Boolean, List[String], () => Any)] = {
    if (test.children.isEmpty) {
      Tree((
        true,
        test.value.name :: revStringPath,
        () => test.value.thunkTree.run(revIntPath.reverse)
      ))
    }
    else{
      Tree(
        (false, test.value.name :: revStringPath, () => ()),
        test.children.zipWithIndex.map{case (c, i) =>
          recTests(c, i :: revIntPath, c.value.name :: revStringPath)
        }:_*
      )
    }
  }
}
