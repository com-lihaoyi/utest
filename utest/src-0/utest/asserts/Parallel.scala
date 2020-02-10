package utest
package asserts

import scala.quoted.{ given, _ }

import scala.concurrent.duration._
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
 * Asserts which only make sense when running on multiple threads.
 */
trait ParallelVersionSpecific {

  def eventuallyProxy(exprs: Expr[Seq[Boolean]], ri: Expr[RetryInterval], rm: Expr[RetryMax])(using ctx: QuoteContext): Expr[Unit] =
    Tracer[Boolean]('{ (esx: Seq[AssertEntry[Boolean]]) => utest.asserts.Parallel.eventuallyImpl(esx: _*)(using $ri, $rm) }, exprs)

  def continuallyProxy(exprs: Expr[Seq[Boolean]], ri: Expr[RetryInterval], rm: Expr[RetryMax])(using ctx: QuoteContext): Expr[Unit] =
    Tracer[Boolean]('{ (esx: Seq[AssertEntry[Boolean]]) => utest.asserts.Parallel.continuallyImpl(esx: _*)(using $ri, $rm) }, exprs)

}
