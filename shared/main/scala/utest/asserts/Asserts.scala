package utest
package asserts

import scala.reflect.macros.Context
import scala.util.{Failure, Success, Try, Random}

/**
 * Macro implementation that provides rich error
 * message for boolean expression assertion.
 */
object Asserts {

  def assertProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    TraceLogger(c)(q"utest.asserts.Asserts.assertImpl", exprs:_*)
  }

  def assertImpl(funcs: (String, (LoggedValue => Unit) => Boolean)*) = {
    for ((src, func) <- funcs){
      val logged = collection.mutable.Buffer.empty[LoggedValue]
      val res = Try(func(logged.append(_)))
      res match{
        case Success(true) => // yay
        case Success(false) => TraceLogger.throwError(src, logged)
        case Failure(e) => TraceLogger.throwError(src, logged, e)
      }
    }
  }
}



