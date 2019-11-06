package utest
package asserts

import scala.quoted.{ given, _ }
import scala.reflect.ClassTag
import scala.compiletime.testing._

import utest.framework.StackMarker

import scala.annotation.{StaticAnnotation, tailrec}
import scala.collection.mutable

/**
 * Macro implementation that provides rich error
 * message for boolean expression assertion.
 */
object Asserts extends AssertsCommons {
  def assertProxy(exprs: Expr[Seq[Boolean]])(given ctx: QuoteContext): Expr[Unit] =
    Tracer[Boolean]('{ (esx: Seq[AssertEntry[Boolean]]) => utest.asserts.Asserts.assertImpl(esx: _*) }, exprs)

  def assertMatchProxy(t: Expr[Any], pf: Expr[PartialFunction[Any, Unit]])(given ctx: QuoteContext): Expr[Unit] = {
    val code = s"${Tracer.codeOf(t)} match { ${Tracer.codeOf(pf)} }"
    Tracer.traceOneWithCode[Any, Unit]('{ (x: AssertEntry[Any]) => utest.asserts.Asserts.assertMatchImpl(x)($pf) }, t, code)
  }

  def interceptProxy[T](exprs: Expr[Unit])(given ctx: QuoteContext, tpe: Type[T]): Expr[T] = {
    import ctx.tasty.{ given, _ }
    val clazz = Literal(Constant.ClassTag[T](given tpe.unseal.tpe))
    Tracer.traceOne[Unit, T]('{ (x: AssertEntry[Unit]) =>
      utest.asserts.Asserts.interceptImpl[$tpe](x)(ClassTag(${clazz.seal.cast[Class[T]]})) }, exprs)
  }

  def compileErrorImpl(errors: List[Error], snippet: String): CompileError =
    errors.headOption.map { err =>
      val posStr = s"${err.lineContent}\n${" " * err.column}^"
      err.kind match
        case ErrorKind.Parser => CompileError.Parse(posStr, err.message)
        case ErrorKind.Typer => CompileError.Type(posStr, err.message)
    }.getOrElse(Util.assertError(s"compileError check failed to have a compilation error when compiling\n$snippet", Nil))
}


trait Asserts{
  import Asserts._

    /**
    * Provides a nice syntax for asserting things are equal, that is pretty
    * enough to embed in documentation and examples
    */
  implicit class ArrowAssert(lhs: Any){
    def ==>[V](rhs: V) = {
      (lhs, rhs) match{
          // Hack to make Arrays compare sanely; at some point we may want some
          // custom, extensible, typesafe equality check but for now this will do
          case (lhs: Array[_], rhs: Array[_]) =>
            Predef.assert(lhs.toSeq == rhs.toSeq, s"==> assertion failed: ${lhs.toSeq} != ${rhs.toSeq}")
          case (lhs, rhs) =>
            Predef.assert(lhs == rhs, s"==> assertion failed: $lhs != $rhs")
        }
    }
  }

  /**
    * Asserts that the given expression fails to compile, and returns a
    * [[utest.CompileError]] containing the message of the failure. If the expression
    * compile successfully, this macro itself will raise a compilation error.
    */
  inline def compileError(inline expr: => String): CompileError = compileErrorImpl(typeCheckErrors(expr), expr)

  /**
    * Checks that one or more expressions are true; otherwises raises an
    * exception with some debugging info
    */
  inline def assert(exprs: => Boolean*): Unit = ${assertProxy('exprs)}

  /**
    * Checks that one or more expressions all become true within a certain
    * period of time. Polls at a regular interval to check this.
    */
  inline def eventually(exprs: => Boolean*)(given ri: => RetryInterval, rm: => RetryMax): Unit =
    ${Parallel.eventuallyProxy('exprs, 'ri, 'rm)}
  /**
    * Checks that one or more expressions all remain true within a certain
    * period of time. Polls at a regular interval to check this.
    */
  inline def continually(exprs: => Boolean*)(given ri: => RetryInterval, rm: => RetryMax): Unit =
    ${Parallel.continuallyProxy('exprs, 'ri, 'rm)}

  /**
    * Asserts that the given value matches the PartialFunction. Useful for using
    * pattern matching to validate the shape of a data structure.
    */
  inline def assertMatch(t: => Any)(pf: => PartialFunction[Any, Unit]): Unit = ${assertMatchProxy('t, 'pf)}

  /**
    * Asserts that the given block raises the expected exception. The exception
    * is returned if raised, and an `AssertionError` is raised if the expected
    * exception does not appear.
    */
  inline def intercept[T](exprs: => Unit): T = ${interceptProxy[T]('exprs)}

  @tailrec final def retry[T](n: Int)(body: => T): T = {
    try body
    catch{case e: Throwable =>
      if (n > 0) retry(n-1)(body)
      else throw e
    }
  }
}

