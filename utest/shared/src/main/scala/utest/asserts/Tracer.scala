package utest
package asserts
import scala.reflect.macros.Context
import acyclic.file
/**
 * Macro implementation to take a block of code and trace through it,
 * converting it into an [[AssertEntry]] and inserting debug loggers.
 */
object Tracer{
  def wrapWithLoggedValue(c: Context)(tree: c.Tree,
                                      loggerName: c.TermName,
                                      tpe: c.Type) = {
    import c.universe._
    val tempName = c.fresh(newTermName("$temp"))
    q"""{
      val $tempName = $tree
      $loggerName(utest.framework.LoggedValue(
        ${tree.toString()},
        ${show(tpe)},
        $tempName
      ))
      $tempName
    }"""
  }
  def apply[T](c: Context)(func: c.Tree, exprs: c.Expr[T]*): c.Expr[Unit] = {
    import c.universe._
    val loggerName = c.fresh(newTermName("$log"))

    import compat._
    object tracingTransformer extends Transformer {
      override def transform(tree: Tree): Tree = {

        tree match {
          case i @ Ident(name)
            if i.symbol.pos != NoPosition
            && i.pos != NoPosition
            && i.symbol.pos.source == i.pos.source
            && !i.symbol.isMethod =>
            // only trace identifiers coming from the same file,
            // since those are the ones people probably care about
            wrapWithLoggedValue(c)(tree, loggerName, tree.tpe.widen)
          case i: Typed =>
            i.tpe match {
              case t: AnnotatedType
                if t.annotations.exists(_.tpe =:= typeOf[utest.asserts.Show]) =>

                val newTpe =
                  if (t.annotations.length == 1) t.underlying
                  else AnnotatedType(
                    t.annotations.filterNot(_.tpe =:= typeOf[utest.asserts.Show]),
                    t.underlying
                  )

                wrapWithLoggedValue(c)(tree, loggerName, newTpe.widen)
              case _ => super.transform(tree)
            }

          case _ => super.transform(tree)
        }
      }
    }

    val trees = exprs.map(expr =>
      q"${expr.tree.pos.lineContent.trim} -> (($loggerName: ${tq""}) => ${tracingTransformer.transform(expr.tree)})"
    )

    c.Expr[Unit](c.resetLocalAttrs(q"""$func(..$trees)"""))
  }
}
