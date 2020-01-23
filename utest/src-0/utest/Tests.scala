package utest

import scala.quoted.{ given, _ }
import scala.tasty._

import utest.framework.{TestCallTree, Tree => UTree }
import scala.collection.mutable

trait TestsVersionSpecific {
  import TestsVersionSpecific._
  inline def apply(inline expr: Unit): Tests = ${testsImpl('expr)}
}

object TestsVersionSpecific {
  def testsImpl(body: Expr[Any])(given helpers: TestBuilder): Expr[Tests] = {
    import helpers._, helpers.qc.tasty._
    val bTree = body.unseal
    val res = processTests(bTree)
    res
  }
}

