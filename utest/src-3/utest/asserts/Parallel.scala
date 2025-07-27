package utest
package asserts

import scala.quoted._

import scala.concurrent.duration._
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
 * Asserts which only make sense when running on multiple threads.
 */
trait ParallelVersionSpecific {

  def eventuallyProxy(expr: Expr[Boolean], ri: Expr[RetryInterval], rm: Expr[RetryMax])(using ctx: Quotes): Expr[Unit] =
    Tracer.single[Boolean]('{ (esx: Seq[AssertEntry[Boolean]]) => utest.asserts.Parallel.eventuallyImpl(esx: _*)(using $ri, $rm) }, expr)

  def continuallyProxy(expr: Expr[Boolean], ri: Expr[RetryInterval], rm: Expr[RetryMax])(using ctx: Quotes): Expr[Unit] =
    Tracer.single[Boolean]('{ (esx: Seq[AssertEntry[Boolean]]) => utest.asserts.Parallel.continuallyImpl(esx: _*)(using $ri, $rm) }, expr)

}
