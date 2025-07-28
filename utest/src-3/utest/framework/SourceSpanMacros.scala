package utest.framework

import scala.language.implicitConversions
import scala.quoted.*


trait SourceSpanMacros {
  inline implicit def generate[T](v: => T): SourceSpan[T] = ${ SourceSpanMacros.text('v) }
  inline def apply[T](v: => T): SourceSpan[T] = ${ SourceSpanMacros.text('v) }
}
object SourceSpanMacros {
  def text[T: Type](v: Expr[T])(using quotes: Quotes): Expr[SourceSpan[T]] = {
    import quotes.reflect.*
    '{
      new SourceSpan[T](
        $v,
        ${Expr(v.asTerm.pos.sourceFile.path)},
        ${Expr(v.asTerm.pos.start)},
        ${Expr(v.asTerm.pos.end)},
      )
    }
  }

}