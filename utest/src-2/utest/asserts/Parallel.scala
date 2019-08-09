package utest
package asserts
// import acyclic.file
import scala.concurrent.duration._
import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.util.{Failure, Success, Try}

/**
 * Asserts which only make sense when running on multiple threads.
 */
object Parallel extends ParallelCommons {

  def eventuallyProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    Tracer[Boolean](c)(q"utest.asserts.Parallel.eventuallyImpl", exprs:_*)
  }

  def continuallyProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    Tracer[Boolean](c)(q"utest.asserts.Parallel.continuallyImpl", exprs:_*)
  }
}

