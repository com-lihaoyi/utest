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

    resolveQueryIndices(tests, query, Nil) match{
      case Left(errors) => throw new utest.NoSuchTestException(errors:_*)
      case Right(resolution) =>
        val thunkTree = collectQueryTerminals(tests, resolution, Nil, Nil)

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

        evaluateFutureTree(forced)
    }

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


  def evaluateFutureTree[T](t: Tree[() => Future[T]])
                           (implicit ec: concurrent.ExecutionContext): Future[Tree[T]] = {
    for{
      v <- t.value()
      childValues <- Future.traverse(t.children.toSeq)(evaluateFutureTree(_))
    } yield Tree(v, childValues:_*)
  }

  /**
    * Takes a query tree and resolves the strings against the test tree of a
    * test suite, either returning the query trees strings converted into
    * integer indices into the test tree, or the list of paths which the query
    * tree tried to access but did not exist.
    */
  def resolveQueryIndices(test: Tree[Test],
                          query: Seq[Tree[String]],
                          revStringPath: List[String]): Either[Seq[Seq[String]], Seq[Tree[Int]]] = {
    val strToIndex = test.children.map(_.value.name).zipWithIndex.toMap

    val childResults = for(q <- query) yield {

      strToIndex.get(q.value) match{
        case None => Left(Seq((q.value :: revStringPath).reverse))
        case Some(index) =>
          resolveQueryIndices(test.children(index), q.children, revStringPath) match{
            case Right(res) => Right(Tree(index, res:_*))
            case Left(l) => Left(l)
          }
      }
    }

    val left = collection.mutable.Buffer.empty[Seq[String]]
    val right = collection.mutable.Buffer.empty[Tree[Int]]
    childResults.foreach{
      case Left(l) => left.appendAll(l)
      case Right(r) => right.append(r)
    }

    if (left.nonEmpty) Left(left)
    else Right(right)
  }

  /**
    * Recurses into the test tree using a pre-[[resolveQueryIndices]]ed query tree, going
    * straight to the terminal nodes of the query tree before handing over to
    * [[collectTestNodes]] to collect all the tests within those nodes.
    */
  def collectQueryTerminals(test: Tree[Test],
                            query: Seq[Tree[Int]],
                            revIntPath: List[Int],
                            revStringPath: List[String]): Tree[(Boolean, List[String], () => Any)] = {

    if (query.isEmpty) collectTestNodes(test, revIntPath, revStringPath)
    else{
      val children = for(subquery <- query) yield collectQueryTerminals(
        test.children(subquery.value),
        subquery.children,
        subquery.value :: revIntPath,
        test.value.name :: revStringPath
      )

      Tree((false, test.value.name :: revStringPath, () => ()), children:_*)
    }
  }

  /**
    * Pick up all terminal nodes within the given test tree, so they can be
    * executed and their results recorded.
    */
  def collectTestNodes(test: Tree[Test],
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
          collectTestNodes(c, i :: revIntPath, c.value.name :: revStringPath)
        }:_*
      )
    }
  }
}
