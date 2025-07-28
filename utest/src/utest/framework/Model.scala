package utest
package framework


import scala.util.Try
import scala.language.experimental.macros

case class TestPath(value: Seq[String])
object TestPath{
  @annotation.compileTimeOnly(
    "TestPath is only available within a uTest suite, and not outside."
  )
  implicit def synthetic: TestPath = ???
}


/**
  * The executable portion of a tree of tests. Each node contains an
  * executable, which when run either returns a Left(result) or a
  * Right(sequence) of child nodes which you can execute.
  */
class TestCallTree(inner: => Either[Any, IndexedSeq[TestCallTree]]) {

  def evalInner() =
    inner

  def mapInner(f: Either[Any, IndexedSeq[TestCallTree]] => Either[Any, IndexedSeq[TestCallTree]]): TestCallTree =
    new TestCallTree(f(inner))

  def prefix: TestCallTree =
    new TestCallTree(Right(IndexedSeq.empty[TestCallTree] :+ this))

  /**
   * Runs the test in this [[TestCallTree]] at the specified `path`. Called
   * by the [[TestCallTree.run]] method and usually not called manually.
   */
  def run(path: List[Int]): Any = {
    path match {
      case head :: tail =>
        val Right(children) = StackMarker.dropOutside(inner)
        children(head).run(tail)
      case Nil =>
        val Left(res) = StackMarker.dropOutside(inner)
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
