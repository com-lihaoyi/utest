package utest
package asserts

import scala.quoted._

/**
 * Macro implementation to take a block of code and trace through it,
 * converting it into an [[AssertEntry]] and inserting debug loggers.
 */
object Tracer {

  def traceOne[I, O](func: Expr[AssertEntry[I] => O], expr: Expr[I])(using TracerHelper, Type[I]): Expr[O] =
    traceOneWithCode(func, expr, codeOf(expr))

  def traceOneWithCode[I, O](func: Expr[AssertEntry[I] => O], expr: Expr[I], code: String)(using h: TracerHelper, tt: Type[I]): Expr[O] = {
    import h._, h.ctx.tasty._
    val tree = makeAssertEntry(expr, code)
    Expr.betaReduce(func)(tree)
  }

  def apply[T](func: Expr[Seq[AssertEntry[T]] => Unit], exprs: Expr[Seq[T]])(using ctx: QuoteContext, tt: Type[T]): Expr[Unit] = {
    val h = new TracerHelper
    import h._, h.ctx.tasty.{given _, _}


    exprs match {
      case Varargs(ess) =>
        val trees: Expr[Seq[AssertEntry[T]]] = Expr.ofSeq(ess.map(e => makeAssertEntry(e, codeOf(e))))
        Expr.betaReduce(func)(trees)

      case _ => throw new RuntimeException(s"Only varargs are supported. Got: ${exprs.unseal}")
    }
  }

  def codeOf[T](expr: Expr[T])(using h: TracerHelper): String = {
    import h.ctx.tasty._
    h.ctx.tasty.positionOps.extension_sourceCode(
      h.ctx.tasty.TreeOps.extension_pos(
        expr.unseal(using h.ctx)
      )(using h.ctx.tasty.given_Context)
    )
    // TODO: replace the above with expr.unseal.pos.sourceCode
    // see lampepfl/dotty#8623
  }
}

class TracerHelper(using val ctx: QuoteContext) {
  import ctx.tasty.{ given _, _ }
  import StringUtilHelpers._

  def tracingMap(logger: Expr[TestValue => Unit]) = new TreeMap {
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

          wrapWithLoggedValue(tree, logger, tree.tpe.widen)

        // Don't worry about multiple chained annotations for now...
        case Typed(_, tpt) =>
          tpt.tpe match {
            case AnnotatedType(underlying, annot) if annot.tpe =:= typeOf[utest.asserts.Show] =>
              wrapWithLoggedValue(tree, logger, underlying.widen)
            case _ => super.transformTerm(tree)
          }

        // Don't recurse and trace the LHS of assignments
        case t@Assign(lhs, rhs) => Assign.copy(t)(lhs, super.transformTerm(rhs))

        case _ => super.transformTerm(tree)
      }
    }
  }

  def wrapWithLoggedValue(tree: ctx.tasty.Term, logger: Expr[TestValue => Unit], tpe: ctx.tasty.Type) = {
    import ctx.tasty._
    val tpeString =
      try tpe.show
      catch
        case _ => tpe.toString // Workaround lampepfl/dotty#8858
    tree.seal match {
      case '{ $x: $t } =>
        '{
          val tmp: $t = $x
          $logger(TestValue(
            ${Expr(tree.show)},
            ${Expr(stripScalaCorePrefixes(tpeString))},
            tmp
          ))
          tmp
        }.unseal
    }
  }

  def makeAssertEntry[T](expr: Expr[T], code: String)(using scala.quoted.Type[T]) =
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

given (using QuoteContext) as TracerHelper = new TracerHelper
given (using h: TracerHelper) as QuoteContext = h.ctx
