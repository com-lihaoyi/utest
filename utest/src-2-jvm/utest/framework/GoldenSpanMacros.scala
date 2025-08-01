package utest.framework

import scala.language.experimental.macros
import scala.reflect.internal.util.RangePosition

trait GoldenSpanMacros {
  implicit def generate[T](v: T): GoldenFix.Span[T] = macro GoldenSpanMacros.text[T]

  def apply[T](v: T): GoldenFix.Span[T] = macro GoldenSpanMacros.text[T]
}

object GoldenSpanMacros {
  object Compat {
    type Context = scala.reflect.macros.blackbox.Context

    def enclosingOwner(c: Context) = c.internal.enclosingOwner

    def enclosingParamList(c: Context): List[List[c.Symbol]] = {
      def nearestEnclosingMethod(owner: c.Symbol): c.Symbol =
        if (owner.isMethod) owner
        else if (owner.isClass) owner.asClass.primaryConstructor
        else nearestEnclosingMethod(owner.owner)

      nearestEnclosingMethod(enclosingOwner(c)).asMethod.paramLists
    }
  }
  def text[T: c.WeakTypeTag](c: Compat.Context)(v: c.Expr[T]): c.Expr[GoldenFix.Span[T]] = {
    import c.universe._
    val (start, end) = if (v.tree.pos.isInstanceOf[RangePosition]) {
      val r = v.tree.pos.asInstanceOf[RangePosition]
      (r.start, r.end)
    } else (-1, -1)
    val tree = q"""${c.prefix}(${v.tree}, ${v.tree.pos.source.path}, $start, $end)"""
    c.Expr[GoldenFix.Span[T]](tree)
  }
}