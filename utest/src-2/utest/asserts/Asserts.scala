package utest
package asserts
//import acyclic.file

import scala.annotation.{StaticAnnotation, tailrec}
import scala.collection.mutable
import scala.reflect.macros.{Context, ParseException, TypecheckException}
import scala.util.{Failure, Random, Success, Try}
import scala.reflect.ClassTag
import scala.reflect.internal.util.{OffsetPosition, Position}
import scala.reflect.internal.util.OffsetPosition
import scala.reflect.macros.{Context, TypecheckException}
import scala.language.experimental.macros

/**
 * Macro implementation that provides rich error
 * message for boolean expression assertion.
 */
trait AssertsCompanionVersionSpecific {
  def assertCompileError(c: Context)(expr: c.Expr[String]): c.Expr[CompileError] = {
    import c.universe._
    val macrocompat = new MacroCompat(c)
    import macrocompat._
    def calcPosMsg(pos: scala.reflect.api.Position) = {
      if (pos == NoPosition) ""
      else pos.lineContent + "\n" + (" " * pos.column) + "^"
    }
    val stringStart =
      expr.tree
         .pos
         .lineContent
         .slice(expr.tree.pos.column, expr.tree.pos.column + 2)


    val quoteOffset = if (stringStart == "\"\"") 2 else 0

    expr.tree match {
      case Literal(Constant(s: String)) =>
        try{

          val tree = c.parse(s)
          for(x <- tree if x.pos != NoPosition){
            import compat._
            x.pos = new OffsetPosition(
              expr.tree.pos.source,
              x.pos.point + expr.tree.pos.point + quoteOffset
            ).asInstanceOf[c.universe.Position]
          }

          val tree2 = c.typeCheck(tree)
          val compileTimeOnlyType = typeOf[scala.reflect.internal.annotations.compileTimeOnly]

          val compileTimeOnlyTree = tree2.collect{ case t =>
            Option(t.symbol)
              .map(_.annotations)
              .toSeq
              .flatten
              .filter(_.tpe =:= compileTimeOnlyType)
              .map(t -> _)
          }.flatten


         compileTimeOnlyTree.headOption match{
            case Some((tree, annot)) =>
              val msg = annot.scalaArgs.head match{
                case Literal(Constant(s: String)) => s
                case t => t.toString
              }
              c.Expr[CompileError](
                q"""utest.CompileError.CompileTimeOnly(${calcPosMsg(tree.pos)}, $msg)"""
              )
            case None =>
              c.abort(
                c.enclosingPosition,
                "assertCompileError check failed to have a compilation error"
              )
          }

        } catch{
          case TypecheckException(pos, msg) =>
            c.Expr[CompileError](q"""utest.CompileError.Type(${calcPosMsg(pos)}, $msg)""")
          case ParseException(pos, msg) =>
            c.Expr[CompileError](q"""utest.CompileError.Parse(${calcPosMsg(pos)}, $msg)""")
          case e: Exception =>
            println("SOMETHING WENT WRONG LOLS " + e)
            throw e
        }
      case e =>
        c.abort(
          expr.tree.pos,
          s"You can only have literal strings in assertCompileError, not ${expr.tree}"
        )
    }
  }

  def assertProxy(c: Context)(expr: c.Expr[Boolean]): c.Expr[Unit] = {
    import c.universe._
    Tracer[Boolean](c)(q"utest.asserts.Asserts.assertImpl", expr)
  }


  def assertAllProxy(c: Context)(expr: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    Tracer[Boolean](c)(q"utest.asserts.Asserts.assertImpl", expr:_*)
  }

  def assertThrowsProxy[T: c.WeakTypeTag]
                    (c: Context)
                    (expr: c.Expr[Unit])
                    (t: c.Expr[ClassTag[T]]): c.Expr[T] = {
    import c.universe._
    val typeTree = implicitly[c.WeakTypeTag[T]]

    val x = Tracer[Unit](c)(q"utest.asserts.Asserts.assertThrowsImpl[$typeTree]", expr)
    c.Expr[T](q"$x($t)")
  }

  def assertMatchProxy(c: Context)
                      (t: c.Expr[Any])
                      (pf: c.Expr[PartialFunction[Any, Unit]]): c.Expr[Unit] = {
    import c.universe._
    val x = Tracer[Any](c)(q"utest.asserts.Asserts.assertMatchImpl", t)
    c.Expr[Unit](q"$x($pf)")
  }
}


trait AssertsVersionSpecific {
  /**
    * Asserts that the given expression fails to compile, and returns a
    * [[utest.CompileError]] containing the message of the failure. If the expression
    * compile successfully, this macro itself will raise a compilation error.
    */
  def assertCompileError(expr: String): CompileError = macro Asserts.assertCompileError

  /**
   * Forwarder for `Predef.assert`, for when you want to explicitly write the
   * assert message and don't want or need the fancy smart asserts
   */
  def assert(expr: Boolean, msg: => Any) = Predef.assert(expr, msg)

  /**
   * Checks that the expression is true; otherwise raises an
   * exception with some debugging info
   */
  def assert(expr: Boolean): Unit = macro Asserts.assertProxy

  /**
    * Checks that one or more expressions are true; otherwise raises an
    * exception with some debugging info
    */
  def assertAll(expr: Boolean*): Unit = macro Asserts.assertAllProxy
  /**
    * Checks that one or more expressions all become true within a certain
    * period of time. Polls at a regular interval to check this.
    */
  def assertEventually(expr: Boolean): Unit = macro Parallel.eventuallyProxy
  /**
    * Checks that one or more expressions all remain true within a certain
    * period of time. Polls at a regular interval to check this.
    */
  def assertContinually(expr: Boolean): Unit = macro Parallel.continuallyProxy

  /**
    * Asserts that the given value matches the PartialFunction. Useful for using
    * pattern matching to validate the shape of a data structure.
    */
  def assertMatch(t: Any)(pf: PartialFunction[Any, Unit]): Unit =  macro Asserts.assertMatchProxy


  /**
    * Asserts that the given block raises the expected exception. The exception
    * is returned if raised, and an `AssertionError` is raised if the expected
    * exception does not appear.
    */
  def assertThrows[T: ClassTag](expr: Unit): T = macro Asserts.assertThrowsProxy[T]

}

