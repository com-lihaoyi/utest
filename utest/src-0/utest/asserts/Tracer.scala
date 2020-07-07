package utest
package asserts

import scala.quoted._

/**
 * Macro implementation to take a block of code and trace through it,
 * converting it into an [[AssertEntry]] and inserting debug loggers.
 */
object Tracer {

  def traceOne[I, O](func: Expr[AssertEntry[I] => O], expr: Expr[I])(using QuoteContext, Type[I]): Expr[O] =
    traceOneWithCode(func, expr, codeOf(expr))

  def traceOneWithCode[I, O](func: Expr[AssertEntry[I] => O], expr: Expr[I], code: String)(using qctx: QuoteContext, tt: Type[I]): Expr[O] = {
    val tree = makeAssertEntry(expr, code)
    Expr.betaReduce(func)(tree)
  }

  def apply[T](func: Expr[Seq[AssertEntry[T]] => Unit], exprs: Expr[Seq[T]])(using qctx: QuoteContext, tt: Type[T]): Expr[Unit] = {
    exprs match {
      case Varargs(ess) =>
        val trees: Expr[Seq[AssertEntry[T]]] = Expr.ofSeq(ess.map(e => makeAssertEntry(e, codeOf(e))))
        Expr.betaReduce(func)(trees)

      case _ => throw new RuntimeException(s"Only varargs are supported. Got: ${exprs.unseal}")
    }
  }

  def codeOf[T](expr: Expr[T])(using QuoteContext): String =
    expr.unseal.pos.sourceCode

  private def tracingMap(logger: Expr[TestValue => Unit])(using QuoteContext) =
    import qctx.tasty._
    new TreeMap {
      // Do not descend into definitions inside blocks since their arguments are unbound
      override def transformStatement(tree: Statement)(using ctx: Context): Statement = tree match
        case _: DefDef => tree
        case _ => super.transformStatement(tree)

      override def transformTerm(tree: Term)(implicit ctx: Context): Term = {
        tree match {
          case i @ Ident(name) if i.symbol.pos.exists
            && i.pos.exists
            // only trace identifiers coming from the same file,
            // since those are the ones people probably care about
            && i.symbol.pos.sourceFile == i.pos.sourceFile
            // Don't trace methods, since you cannot just print them "standalone"
            // without providing arguments
            && !i.symbol.isDefDef && !i.symbol.isClassConstructor
            // Don't trace identifiers which are synthesized by the compiler
            // as part of the language implementation
            && !i.symbol.flags.is(Flags.Artifact)
            // Don't trace "magic" identifiers with '$'s in them
            && !name.toString.contains('$') =>

            wrapWithLoggedValue(tree.seal, logger, tree.tpe.widen.seal)

          // Don't worry about multiple chained annotations for now...
          case Typed(_, tpt) =>
            tpt.tpe match {
              case AnnotatedType(underlying, annot) if annot.tpe =:= typeOf[utest.asserts.Show] =>
                wrapWithLoggedValue(tree.seal, logger, underlying.widen.seal)
              case _ => super.transformTerm(tree)
            }

          // Don't recurse and trace the LHS of assignments
          case t@Assign(lhs, rhs) => Assign.copy(t)(lhs, super.transformTerm(rhs))

          case _ => super.transformTerm(tree)
        }
      }
    }

  private def wrapWithLoggedValue(expr: Expr[Any], logger: Expr[TestValue => Unit], tpe: Type[?])(using QuoteContext) = {
    val tpeString =
      try tpe.show
      catch
        case _ => tpe.toString // Workaround lampepfl/dotty#8858
    expr match {
      case '{ $x: $t } =>
        '{
          val tmp: $t = $x
          $logger(TestValue(
            ${Expr(expr.show)},
            ${Expr(StringUtilHelpers.stripScalaCorePrefixes(tpeString))},
            tmp
          ))
          tmp
        }.unseal
    }
  }

  private def makeAssertEntry[T](expr: Expr[T], code: String)(using QuoteContext, scala.quoted.Type[T]) =
    def entryBody(logger: Expr[TestValue => Unit]) =
      tracingMap(logger).transformTerm(expr.unseal).seal.cast[T]
    '{AssertEntry(
      ${Expr(code)},
      logger => ${entryBody('logger)})}
}

object StringUtilHelpers {
  def stripScalaCorePrefixes(tpeName: String): String = {
    val pattern = """(?<!\.)(scala|java\.lang)(\.\w+)*\.(?<tpe>\w+)""".r // Match everything under the core `scala` or `java.lang` packages
    pattern.replaceAllIn(tpeName, _.group("tpe"))
  }

  def (str: String) trim: String =
    str.dropWhile(_ == ' ').reverse.dropWhile(_ == ' ').reverse
}
