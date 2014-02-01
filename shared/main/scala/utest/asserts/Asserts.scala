package utest
package asserts

import scala.reflect.macros.Context
import scala.util.Random

/**
 * Macro implementation that provides rich error
 * message for boolean expression assertion.
 */
object Asserts {

  def assertProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    TraceLogger(c)(q"utest.asserts.Asserts.assertImpl", exprs:_*)
  }

  def assertImpl(funcs: ((LoggedValue => Unit) => Boolean)*) = {
    for (func <- funcs){
      val logged = collection.mutable.Buffer.empty[LoggedValue]
      val res = func(logged.append(_))
      if (!res) TraceLogger.throwError("Assert failed: ", logged)
    }
  }
}

object TraceLogger{
  def throwError(msgPrefix: String, logged: Seq[LoggedValue]) = {
    throw LoggedAssertionError(
      msgPrefix + logged.map{
        case LoggedValue(name, tpe, thing) => s"\n$name: $tpe = $thing"
      }.mkString,
      logged
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
            && i.symbol.pos.source == c.enclosingUnit.source =>
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
      q"($loggerName => ${tracingTransformer.transform(expr.tree)})"
    )

    c.Expr[Unit](c.resetLocalAttrs(q"$func(..$trees)"))
  }
}

