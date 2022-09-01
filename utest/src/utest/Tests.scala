package utest

import utest.framework.{TestCallTree, Tree}
import scala.collection.mutable

/**
  * Represents a single hierarchy of tests, arranged in a tree structure, with
  * every node having a name and an associated executable test.
  *
  * The two hierarchies are parallel: thus you can inspect the `nameTree` to
  * browse the test listing without running anything, and once you decide which
  * test to run you can feed the `List[Int]` path of that test in the `nameTree`
  * into the `callTree` to execute it and return the result.
  */
case class Tests(nameTree: Tree[String], callTree: TestCallTree) {

  def prefix(name: String): Tests = {
    val newNameTree = Tree(nameTree.value, nameTree.children.map(_.prefix(name)): _*)
    val newCallTree = callTree.mapInner {
      case Right(ts) => Right(ts.map(_.prefix))
      case l@ Left(_) => l
    }
    Tests(newNameTree, newCallTree)
  }

  def ++(t: Tests): Tests = {
    val newNameTree = Tree(nameTree.value, (nameTree.children ++ t.nameTree.children): _*)
    val newCallTree = new TestCallTree({
      val a = callTree.evalInner()
      val b = t.callTree.evalInner()
      (a, b) match {
        case (Right(x), Right(y)) => Right(x ++ y)
        case (Left (_), Right(y)) => Right(y)
        case (Right(x), Left (_)) => Right(x)
        case (Left (_), Left (y)) => Left(y)
      }
    })
    Tests(newNameTree, newCallTree)
  }
}

object Tests extends TestsVersionSpecific
