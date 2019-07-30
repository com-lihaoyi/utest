package utest
package asserts
//import acyclic.file
import utest.framework.StackMarker

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
object Asserts extends AssertsCommons {
  def compileError(c: Context)(expr: c.Expr[String]): c.Expr[CompileError] = {
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
                "compileError check failed to have a compilation error"
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
          s"You can only have literal strings in compileError, not ${expr.tree}"
        )
    }
  }

  def assertProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    Tracer[Boolean](c)(q"utest.asserts.Asserts.assertImpl", exprs:_*)
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

  def assertMatchProxy(c: Context)
                      (t: c.Expr[Any])
                      (pf: c.Expr[PartialFunction[Any, Unit]]): c.Expr[Unit] = {
    import c.universe._
    val x = Tracer[Any](c)(q"utest.asserts.Asserts.assertMatchImpl", t)
    c.Expr[Unit](q"$x($pf)")
  }
}


trait Asserts{
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
  def compileError(expr: String): CompileError = macro Asserts.compileError
  /**
    * Checks that one or more expressions are true; otherwises raises an
    * exception with some debugging info
    */
  def assert(exprs: Boolean*): Unit = macro Asserts.assertProxy
  /**
    * Checks that one or more expressions all become true within a certain
    * period of time. Polls at a regular interval to check this.
    */
  def eventually(exprs: Boolean*): Unit = macro Parallel.eventuallyProxy
  /**
    * Checks that one or more expressions all remain true within a certain
    * period of time. Polls at a regular interval to check this.
    */
  def continually(exprs: Boolean*): Unit = macro Parallel.continuallyProxy

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
  def intercept[T: ClassTag](exprs: Unit): T = macro Asserts.interceptProxy[T]

  @tailrec final def retry[T](n: Int)(body: => T): T = {
    try body
    catch{case e: Throwable =>
      if (n > 0) retry(n-1)(body)
      else throw e
    }
  }
}

