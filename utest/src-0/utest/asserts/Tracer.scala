package utest
package asserts

import scala.quoted._, scala.quoted.matching._
import delegate scala.quoted._
import scala.tasty._


/**
 * Macro implementation to take a block of code and trace through it,
 * converting it into an [[AssertEntry]] and inserting debug loggers.
 */
object Tracer{
  val wrapWithLoggedValue = given (ctx: QuoteContext) => (tree: ctx.tasty.Term, logger: Expr[TestValue => Unit], tpe: ctx.tasty.Type) => {
    import ctx.tasty._
    type T
    implicit val t: scala.quoted.Type[T] = tpe.seal.asInstanceOf[scala.quoted.Type[T]]
    '{
      val tmp: $t = ${tree.seal}.asInstanceOf[$t]
      $logger(TestValue(
        ${tree.show.toExpr},
        ${tpe.show.toExpr},
        tmp
      ))
      tmp
    }.unseal
  }

  def apply[T](func: Expr[Seq[AssertEntry[T]] => Unit], exprs: Expr[Seq[T]]) given (ctx: QuoteContext, tt: Type[T]): Expr[Unit] = {
    import ctx.tasty._

    def tracingMap(logger: Expr[TestValue => Unit]) = new TreeMap {
      override def transformTerm(tree: Term)(implicit ctx: Context): Term = {
        tree match {
          case i @ Ident(name) if i.symbol.pos.exists
            && i.pos.exists
            // only trace identifiers coming from the same file,
            // since those are the ones people probably care about
            && i.symbol.pos.sourceFile == i.pos.sourceFile
            // Don't trace methods, since you cannot just print them "standalone"
            // without providing arguments
            && !IsDefDefSymbol.unapply(i.symbol).isDefined && !i.symbol.isClassConstructor
            // Don't trace identifiers which are synthesized by the compiler
            // as part of the language implementation
            && !i.symbol.flags.is(Flags.Artifact)
            // Don't trace "magic" identifiers with '$'s in them
            && !name.toString.contains('$') =>

            wrapWithLoggedValue(tree, logger, tree.tpe.widen)
         
          // Don't worry about multiple chained annotations for now...
          case Typed(_, tpt) =>
            tpt.tpe match {
              case Type.AnnotatedType(underlying, annot) if annot.tpe == typeOf[utest.asserts.Show] =>
                wrapWithLoggedValue(tree, logger, underlying.widen)
              case _ => super.transformTerm(tree)
            }
  
          // Don't recurse and trace the LHS of assignments
          case Assign(_, rhs) => super.transformTerm(rhs)

          case _ => super.transformTerm(tree)
        }
      }
    }

    exprs match {
      case ExprSeq(ess) =>
        val trees: Expr[Seq[AssertEntry[T]]] = ess.map(expr =>
          '{AssertEntry(
            ${expr.show.toExpr},
            logger => ${tracingMap('logger).transformTerm(expr.unseal).seal.cast[T]})}
        ).toExprOfSeq

        func(trees)

      case _ => throw new RuntimeException(s"Only varargs are supported. Got: $exprs")
    }
  }
}
