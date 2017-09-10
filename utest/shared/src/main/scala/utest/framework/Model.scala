package utest
package framework


import scala.util.Try
import scala.language.experimental.macros

import utest.PlatformShims

case class TestPath(value: Seq[String])
object TestPath{
  @reflect.internal.annotations.compileTimeOnly(
    "TestPath is only available within a uTest suite, and not outside."
  )
  implicit def synthetic: TestPath = ???
}

/**
  * Represents a single hierarchy of tests, arranged in a tree structure, with
  * every node having a name and an associated executable test.
  *
  * The two hierarchies are parallel: thus you can inspect the `nameTree` to
  * browse the test listing without running anything, and once you decide which
  * test to run you can feed the `List[Int]` path of that test in the `nameTree`
  * into the `callTree` to execute it and return the result.
  */
case class TestHierarchy(nameTree: Tree[String], callTree: TestCallTree)

/**
  * The executable portion of a tree of tests. Each node contains an
  * executable, which when run either returns a Left(result) or a
  * Right(sequence) of child nodes which you can execute.
  */
class TestCallTree(inner: => Either[Any, IndexedSeq[TestCallTree]]){
  /**
   * Runs the test in this [[TestCallTree]] at the specified `path`. Called
   * by the [[TestTreeSeq.run]] method and usually not called manually.
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
