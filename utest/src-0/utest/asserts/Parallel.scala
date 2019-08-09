package utest
package asserts

import scala.quoted._
import delegate scala.quoted._

import scala.concurrent.duration._
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
 * Asserts which only make sense when running on multiple threads.
 */
object Parallel extends ParallelCommons {
  def eventuallyProxy(exprs: Expr[Seq[Boolean]]) given (ctx: QuoteContext): Expr[Unit] =
    Tracer[Boolean]('{ (esx: Seq[AssertEntry[Boolean]]) => utest.asserts.Parallel.eventuallyImpl(esx: _*) }, exprs)

  def continuallyProxy(exprs: Expr[Seq[Boolean]]) given (ctx: QuoteContext): Expr[Unit] =
    Tracer[Boolean]('{ (esx: Seq[AssertEntry[Boolean]]) => utest.asserts.Parallel.continuallyImpl(esx: _*) }, exprs)
}
