package utest

import scala.quoted._

import utest.framework.{TestCallTree, Tree => UTree }
import scala.collection.mutable

trait TestsVersionSpecific {
  import TestsVersionSpecific._
  inline def apply(inline expr: Unit): Tests = ${testsImpl('expr)}
}

object TestsVersionSpecific {
  def testsImpl(body: Expr[Any])(using Quotes): Expr[Tests] = {
    val helpers = new TestBuilder(quotes)
    helpers.processTests(quotes.reflect.Term.of(body))
  }
}
