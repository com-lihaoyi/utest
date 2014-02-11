package utest
package asserts

import scala.reflect.macros.Context
import scala.util.{Failure, Success, Try, Random}
import scala.reflect.ClassTag

/**
 * Macro implementation that provides rich error
 * message for boolean expression assertion.
 */
object Asserts {

  def assertProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    TraceLogger[Boolean](c)(q"utest.asserts.Asserts.assertImpl", exprs:_*)
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

  def interceptProxy[T: c.WeakTypeTag]
                    (c: Context)
                    (exprs: c.Expr[Unit])
                    (t: c.Expr[ClassTag[T]]): c.Expr[T] = {
    import c.universe._
    val typeTree = implicitly[c.WeakTypeTag[T]]

    val x = TraceLogger[Unit](c)(q"utest.asserts.Asserts.interceptImpl[$typeTree]", exprs)
    c.Expr[T](q"$x($t)")

  }

  /**
   * Asserts that the given block raises the expected exception. The exception
   * is returned if raised, and an `AssertionError` is raised if the expected
   * exception does not appear.
   */
  def interceptImpl[T: ClassTag](funcs: (String, (LoggedValue => Unit) => Unit)): T = {
    val (src, func) = funcs
    val logged = collection.mutable.Buffer.empty[LoggedValue]
    try{
      func(logged.append(_))
    } catch {
      case e: T => return e
      case e: Throwable =>
        TraceLogger.throwError(src, logged, e)
    }
    TraceLogger.throwError(src, logged, null)
  }
}



