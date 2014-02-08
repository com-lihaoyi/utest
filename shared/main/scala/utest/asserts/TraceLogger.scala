package utest
package asserts

import scala.reflect.macros.Context

/**
 * Created by haoyi on 2/8/14.
 */
object TraceLogger{
  def throwError(msgPrefix: String, logged: Seq[LoggedValue], cause: Throwable = null) = {
    throw AssertionError(
      msgPrefix + Option(cause).fold("")(e => s"\ncaused by: $e") + logged.map{
        case LoggedValue(name, tpe, thing) => s"\n$name: $tpe = $thing"
      }.mkString,
      logged,
      cause
    )
  }
  def apply(c: Context)(func: c.Tree, exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    val loggerName = c.fresh(newTermName("$log"))
    val tempName = c.fresh(newTermName("$temp"))

    object tracingTransformer extends Transformer {
      override def transform(tree: Tree): Tree = {

        tree match {
          case i @ Ident(name)
            if i.symbol.pos != NoPosition
            && i.symbol.pos.source == i.pos.source =>
            // only trace identifiers coming from the same file,
            // since those are the ones people probably care about
            q"""{
            val $tempName = $tree
            $loggerName(utest.LoggedValue(
              ${tree.toString()},
              ${show(tree.symbol.typeSignature)},
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
