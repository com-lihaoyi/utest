package utest

import scala.quoted._

import utest.framework.{TestCallTree, Tree => UTree }
import scala.collection.mutable

trait TestsVersionSpecific {
  import TestsVersionSpecific._
  // FIXME remove transparent
  //       This there seems to be an issue with case classes in inline defs
  //       [error] -- Error: /__w/dotty/dotty/community-build/community-projects/upickle/upickle/test/src/upickle/example/ExampleTests.scala:248:19
  //       [error] 248 |        case class Wrap(i: Int)
  //       [error]     |                   ^
  //       [error]     |While expanding a macro, a reference to value i was used outside the scope where it was defined
  //       [error]     | This location contains code that was inlined from ExampleTests.scala:248
  transparent inline def apply(inline expr: Unit): Tests = ${testsImpl('expr)}
}

object TestsVersionSpecific {
  def testsImpl(body: Expr[Any])(using Quotes): Expr[Tests] = {
    import quotes.reflect._
    val helpers = new TestBuilder(quotes)
    helpers.processTests(body.asTerm)
  }
}
