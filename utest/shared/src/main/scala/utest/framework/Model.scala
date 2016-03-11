package utest
package framework


import acyclic.file

import scala.util.{Success, Failure, Try}
import scala.concurrent.duration.Deadline
import scala.language.experimental.macros
import scala.concurrent.Future

object Test{
  /**
   * Creates a test from a set of children, a name a a [[TestThunKTree]].
   * Generally called by the [[TestSuite.apply]] macro and doesn't need to
   * be called manually.
   */
  def create(tests: (TestThunkTree => Tree[Test])*)
            (testTree: TestThunkTree): Tree[Test] = {
    new Tree(
      new Test(testTree),
      tests.map{ case v => v(testTree) }
    )
  }
}

/**
 * Represents the metadata around a single test in a [[TestTreeSeq]]. This is
 * a pretty simple data structure, as much of the information related to it
 * comes contextually when traversing the [[utest.framework.TestTreeSeq]] to reach it.
 */
case class Test(TestThunkTree: TestThunkTree)

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
