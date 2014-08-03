package utest
package asserts

import scala.reflect.macros.{ParseException, TypecheckException, Context}
import scala.util.{Failure, Success, Try, Random}
import scala.reflect.ClassTag

/**
 * Macro implementation that provides rich error
 * message for boolean expression assertion.
 */
object Asserts {
  def compileError(c: Context)(expr: c.Expr[String]): c.Expr[CompileError] = {
    import c.universe._
    val Literal(Constant(s: String)) = expr.tree

    try{
      c.typeCheck(c.parse(s))
      c.abort(c.enclosingPosition, "compileError check failed to have a compilation error")
    } catch{
      case TypecheckException(pos, msg) =>
        c.Expr[CompileError](q"""utest.CompileError.Type($msg)""")
      case ParseException(pos, msg) =>
        c.Expr[CompileError](q"""utest.CompileError.Parse($msg)""")
    }
  }

  def assertProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    Tracer[Boolean](c)(q"utest.asserts.Asserts.assertImpl", exprs:_*)
  }

  def assertImpl(funcs: AssertEntry[Boolean]*) = {
    for (entry <- funcs){
      val (value, die) = entry.get()
      if (!value) die(null)
    }
  }

  def interceptProxy[T: c.WeakTypeTag]
                    (c: Context)
                    (exprs: c.Expr[Unit])
                    (t: c.Expr[ClassTag[T]]): c.Expr[T] = {
    import c.universe._
    val typeTree = implicitly[c.WeakTypeTag[T]]

    val x = Tracer[Unit](c)(q"utest.asserts.Asserts.interceptImpl[$typeTree]", exprs)
    c.Expr[T](q"$x($t)")
  }

  /**
   * Asserts that the given block raises the expected exception. The exception
   * is returned if raised, and an `AssertionError` is raised if the expected
   * exception does not appear.
   */
  def interceptImpl[T: ClassTag](entry: AssertEntry[Unit]): T = {
    val (res, logged, src) = entry.run()
    res match{
      case Failure(e: T) => e
      case Failure(e: Throwable) => assertError(src, logged, e)
      case Success(v) => assertError(src, logged, null)
    }
  }

  def assertMatchProxy(c: Context)
                      (t: c.Expr[Any])
                      (pf: c.Expr[PartialFunction[Any, Unit]]): c.Expr[Unit] = {
    import c.universe._
    val x = Tracer[Any](c)(q"utest.asserts.Asserts.assertMatchImpl", t)
    c.Expr[Unit](q"$x($pf)")
  }

  /**
   * Asserts that the given block raises the expected exception. The exception
   * is returned if raised, and an `AssertionError` is raised if the expected
   * exception does not appear.
   */
  def assertMatchImpl(entry: AssertEntry[Any])
                     (pf: PartialFunction[Any, Unit]): Unit = {
    val (value, die) = entry.get()
    if (pf.isDefinedAt(value)) ()
    else die(null)
  }
}



