package utest
package asserts
import scala.reflect.macros.Context
//import acyclic.file
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
      $loggerName(utest.TestValue(
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
            // only trace identifiers coming from the same file,
            // since those are the ones people probably care about
            && i.symbol.pos.source == i.pos.source
            // Don't trace methods, since you cannot just print them "standalone"
            // without providing arguments
            && !i.symbol.isMethod
            // Don't trace identifiers which are synthesized by the compiler
            // as part of the language implementation
            && !i.symbol.isImplementationArtifact
            // Don't trace "magic" identifiers with '$'s in them
            && !name.toString.contains('$') =>

            wrapWithLoggedValue(c)(tree, loggerName, tree.tpe.widen)
          case i: Typed =>
            i.tpe match {
              case t: AnnotatedType
                // Don't worry about multiple chained annotations for now...
                if t.annotations.map(_.tpe) == Seq(typeOf[utest.asserts.Show]) =>

                val newTpe = t.underlying

                wrapWithLoggedValue(c)(tree, loggerName, newTpe.widen)
              case _ => super.transform(tree)
            }

          // Don't recurse and trace the LHS of assignments
          case i: Assign => super.transform(i.rhs)

          case _ => super.transform(tree)
        }
      }
    }

    val trees = exprs.map(expr =>
      q"""utest.asserts.AssertEntry(
        ${expr.tree.pos.lineContent.trim},
        (($loggerName: ${tq""}) => ${tracingTransformer.transform(expr.tree)})
      )"""
    )

    c.Expr[Unit](c.resetLocalAttrs(q"""$func(..$trees)"""))
  }
}
