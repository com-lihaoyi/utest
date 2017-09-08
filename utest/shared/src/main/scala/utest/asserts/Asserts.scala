package utest
package asserts
//import acyclic.file
import scala.annotation.{tailrec, StaticAnnotation}
import scala.reflect.macros.{ParseException, TypecheckException, Context}
import scala.util.{Failure, Success, Try, Random}
import scala.reflect.ClassTag
import scala.reflect.internal.util.{Position, OffsetPosition}
import scala.reflect.internal.util.OffsetPosition
import scala.reflect.macros.{TypecheckException, Context}

import scala.language.experimental.macros

/**
 * Macro implementation that provides rich error
 * message for boolean expression assertion.
 */
object Asserts {
  def compileError(c: Context)(expr: c.Expr[String]): c.Expr[CompileError] = {
    import c.universe._
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


  def assertImpl(funcs: AssertEntry[Boolean]*) = {
    val tries = for (entry <- funcs) yield Try{
      val (value, die) = Util.getAssertionEntry(entry)
      if (!value) die(null)
    }
    val failures = tries.collect{case util.Failure(thrown) => thrown}
    failures match{
      case Seq() => () // nothing failed, do nothing
      case Seq(failure) => throw failure
      case multipleFailures => throw new MultipleErrors(multipleFailures:_*)
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
    val (res, logged, src) = Util.runAssertionEntry(entry)
    res match{
      case Failure(e: T) => e
      case Failure(e: Throwable) => Util.assertError(src, logged, e)
      case Success(v) => Util.assertError(src, logged, null)
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
    val (value, die) = Util.getAssertionEntry(entry)
    if (pf.isDefinedAt(value)) ()
    else die(null)
  }
}


trait Asserts[V[_]]{
  def assertPrettyPrint[T: V](t: T): fansi.Str

  /**
    * Provides a nice syntax for asserting things are equal, that is pretty
    * enough to embed in documentation and examples
    */
  implicit class ArrowAssert[T](lhs: T){
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

