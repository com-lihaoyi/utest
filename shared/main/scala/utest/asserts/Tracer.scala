package utest
package asserts
import acyclic.file
import scala.reflect.macros.Context

/**
 * Macro implementation to take a block of code and trace through it,
 * converting it into an [[AssertEntry]] and inserting debug loggers.
 */
object Tracer{

  def apply[T](c: Context)(func: c.Tree, exprs: c.Expr[T]*): c.Expr[Unit] = {
    import c.universe._
    val loggerName = c.fresh(newTermName("$log"))
    val tempName = c.fresh(newTermName("$temp"))

    object tracingTransformer extends Transformer {
      override def transform(tree: Tree): Tree = {

        tree match {
          case i @ Ident(name)
            if i.symbol.pos != NoPosition
            && i.pos != NoPosition
            && i.symbol.pos.source == i.pos.source =>
            // only trace identifiers coming from the same file,
            // since those are the ones people probably care about
            q"""{
            val $tempName = $tree
            $loggerName(utest.LoggedValue(
              ${tree.toString()},
              ${show(tree.tpe.widen)},
              $tempName
            ))
             $tempName
          }"""
          case _ => super.transform(tree)
        }
      }
    }

    val trees = exprs.map(expr =>
      q"${expr.tree.pos.lineContent.trim} -> ($loggerName => ${tracingTransformer.transform(expr.tree)})"
    )

    c.Expr[Unit](c.resetLocalAttrs(q"""$func(..$trees)"""))
  }
}
