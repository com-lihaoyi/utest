package utest.framework

import scala.language.implicitConversions
import scala.quoted.*


trait GoldenSpanMacros {
  inline implicit def generate[T](v: => T): GoldenFix.Span[T] = ${ GoldenSpanMacros.text('v) }
  inline def apply[T](v: => T): GoldenFix.Span[T] = ${ GoldenSpanMacros.text('v) }
}
object GoldenSpanMacros {
  def text[T: Type](v: Expr[T])(using quotes: Quotes): Expr[GoldenFix.Span[T]] = {
    import quotes.reflect.*
    '{
      new GoldenFix.Span[T](
        $v,
        ${Expr(v.asTerm.pos.sourceFile.path)},
        ${Expr(v.asTerm.pos.start)},
        ${Expr(v.asTerm.pos.end)},
      )
    }
  }

}