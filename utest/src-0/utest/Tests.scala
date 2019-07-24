package utest

import scala.quoted._
import delegate scala.quoted._
import scala.tasty._

import utest.framework.{TestCallTree, Tree => UTree }
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
case class Tests(nameTree: UTree[String], callTree: TestCallTree)
object Tests{
  inline def apply(expr: => Unit): Tests = ${testsImpl('expr)}

  def testsImpl(body: Expr[Any]) given (helpers: TestBuilder): Expr[Tests] = {
    import helpers._, helpers.qc.tasty._
    import delegate helpers._

    println(s"In:\n${body.show}")
    val bTree = body.unseal
    val res = processTests(bTree)
    println(s"Out:\n${res.show}\n")
    res
  }

  // def testsImpl(body: Expr[Unit]) given QuoteContext: Expr[Tests] =
  //   '{Tests(Tree[String]("root"), TestCallTree(Left(())))}
}

