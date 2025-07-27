package utest
package asserts

import scala.quoted._
import scala.reflect.ClassTag
import scala.compiletime.testing._

import utest.framework.StackMarker

import scala.annotation.{StaticAnnotation, tailrec}
import scala.collection.mutable

/**
 * Macro implementation that provides rich error
 * message for boolean expression assertion.
 */
trait AssertsCompanionVersionSpecific {
  def assertProxy(expr: Expr[Boolean])(using ctx: Quotes): Expr[Unit] = {
    Tracer.single[Boolean](
      '{ (esx: Seq[AssertEntry[Boolean]]) => utest.asserts.Asserts.assertImpl(esx: _*) },
      expr
    )
  }

  def assertAllProxy(exprs: Expr[Seq[Boolean]])(using ctx: Quotes): Expr[Unit] = {
    Tracer[Boolean] ('{ (esx: Seq[AssertEntry[Boolean]]) => utest.asserts.Asserts.assertImpl(esx: _*) }, exprs)
  }

  def assertMatchProxy(t: Expr[Any], pf: Expr[PartialFunction[Any, Unit]])(using ctx: Quotes): Expr[Unit] = {
    val code = s"${Tracer.codeOf(t)} match { ${Tracer.codeOf(pf)} }"
    Tracer.traceOneWithCode[Any, Unit]('{ (x: AssertEntry[Any]) => utest.asserts.Asserts.assertMatchImpl(x)($pf) }, t, code)
  }

  def assertThrowsProxy[T](expr: Expr[Unit])(using Quotes, Type[T]): Expr[T] = {
    import quotes.reflect._
    val clazz = Literal(ClassOfConstant(TypeRepr.of[T]))
    Tracer.traceOne[Unit, T]('{ (x: AssertEntry[Unit]) =>
      utest.asserts.Asserts.assertThrowsImpl[T](x)(ClassTag(${clazz.asExprOf[Class[T]]})) }, expr)
  }

  def compileErrorImpl(errors: List[Error], snippet: String): CompileError =
    errors.headOption.map { err =>
      val posStr = s"${err.lineContent}\n${" " * err.column}^"
      err.kind match
        case ErrorKind.Parser => CompileError.Parse(posStr, err.message)
        case ErrorKind.Typer => CompileError.Type(posStr, err.message)
    }.getOrElse(Util.assertError(s"assertCompileError check failed to have a compilation error when compiling\n$snippet", Nil))
}


trait AssertsVersionSpecific {
  import Asserts._

  /**
    * Asserts that the given expression fails to compile, and returns a
    * [[utest.CompileError]] containing the message of the failure. If the expression
    * compile successfully, this macro itself will raise a compilation error.
    */
  transparent inline def assertCompileError(inline expr: String): CompileError = compileErrorImpl(typeCheckErrors(expr), expr)

  /**
   * Checks that the expression is true; otherwise raises an
   * exception with some debugging info
   */
  inline def assert(inline expr: Boolean): Unit = ${Asserts.assertProxy('expr)}

  /**
    * Checks that one or more expressions are true; otherwise raises an
    * exception with some debugging info
    */
  inline def assertAll(inline expr: Boolean*): Unit = ${Asserts.assertAllProxy('expr)}

  /**
   * Forwarder for `Predef.assert`, for when you want to explicitly write the
   * assert message and don't want or need the fancy smart asserts
   */
  def assert(expr: Boolean, msg: => Any) = Predef.assert(expr, msg)

  /**
    * Checks that one or more expressions all become true within a certain
    * period of time. Polls at a regular interval to check this.
    */
  inline def assertEventually(inline expr: Boolean)(using ri: => RetryInterval, rm: => RetryMax): Unit =
    ${Parallel.eventuallyProxy('expr, 'ri, 'rm)}
  /**
    * Checks that one or more expressions all remain true within a certain
    * period of time. Polls at a regular interval to check this.
    */
  inline def assertContinually(inline expr: Boolean)(using ri: => RetryInterval, rm: => RetryMax): Unit =
    ${Parallel.continuallyProxy('expr, 'ri, 'rm)}

  /**
    * Asserts that the given value matches the PartialFunction. Useful for using
    * pattern matching to validate the shape of a data structure.
    */
  inline def assertMatch(inline t: Any)(pf: => PartialFunction[Any, Unit]): Unit = ${Asserts.assertMatchProxy('t, 'pf)}

  /**
    * Asserts that the given block raises the expected exception. The exception
    * is returned if raised, and an `AssertionError` is raised if the expected
    * exception does not appear.
    */
  inline def assertThrows[T](inline expr: Unit): T = ${Asserts.assertThrowsProxy[T]('expr)}
}

