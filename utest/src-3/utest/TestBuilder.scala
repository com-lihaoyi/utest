package utest

import scala.quoted.{ Type => QType, _ }
import scala.util.Success

import utest.framework.{TestCallTree, Tree => UTree, TestPath }


object TestBuilder:

  def processTests(using Quotes)(body: Expr[Unit]): Expr[Tests] =
    import quotes.reflect._
    body.asTerm match {
      case Stats(tests, setupStats) =>
        val (nestedNameTrees, nestedBodyTrees) = buildTestsTrees(tests, Vector())
        val nestedNameTreesSeq = Varargs(nestedNameTrees)
        val callTree = testCallTreeExpr(nestedBodyTrees, setupStats)
        '{ Tests(UTree[String]("", $nestedNameTreesSeq: _*), $callTree) }
    }

  private def buildTestsTrees(using Quotes)(tests: List[quotes.reflect.Apply], path: Seq[String]): (List[Expr[UTree[String]]], List[Expr[TestCallTree]]) =
    if tests.isEmpty then Nil -> Nil
    else tests.zipWithIndex.foldLeft((List.empty[Expr[UTree[String]]], List.empty[Expr[TestCallTree]])) {
      case ((namesForest, callsForest), (nextTest, id)) =>
        val (name, body) = processTest(nextTest, path, id)
        (namesForest :+ name, callsForest :+ body)
    }

  private def testCallTreeExpr(using Quotes)(nestedBodyTrees: List[Expr[TestCallTree]], setupStats: List[quotes.reflect.Statement]): Expr[TestCallTree] =
    import quotes.reflect._
    val assertInliner = new TreeMap {
      override def transformTerm(term: Term)(owner: Symbol): Term = scala.util.Try(term.asExpr) match {
          case Success(expr) => expr match
            case '{utest.assert(${_}*) } => Inlined(None,Nil,term) //Inlined results in proper line number generation
            case _ => super.transformTerm(term)(owner)
          case _ => super.transformTerm(term)(owner)
      }
    }
    val statsWithInlinedAsserts = assertInliner.transformStats(setupStats)(Symbol.spliceOwner)
    val inner =
      if nestedBodyTrees.nonEmpty then Block(statsWithInlinedAsserts, '{Right(${Expr.ofList(nestedBodyTrees)}.toIndexedSeq)}.asTerm)
      else
        Block(statsWithInlinedAsserts.dropRight(1), '{Left(${statsWithInlinedAsserts.last.asExpr})}.asTerm)
    '{ TestCallTree(${inner.asExprOf[Either[Any, IndexedSeq[TestCallTree]]]}) }

  private def processTest(using Quotes)(test: quotes.reflect.Apply, pathOld: Seq[String], index: Int): (Expr[UTree[String]], Expr[TestCallTree]) =
    import quotes.reflect._
    test match {
      case Test(nameOpt, nestedTests, setupStatsRaw) =>
        val name = nameOpt.getOrElse(index.toString)
        val path = pathOld :+ name
        val pathExpr = Expr(path)
        val (nestedNameTrees, nestedBodyTrees) = buildTestsTrees(nestedTests, path)

        object testPathMap extends TreeMap {
          override def transformTerm(t: Term)(owner: Symbol): Term =
            t.tpe.widen match {
              case _: MethodType | _: PolyType => super.transformTerm(t)(owner)
              case _ => t.asExpr match {
                case '{ TestPath.synthetic } => '{ TestPath($pathExpr) }.asTerm
                case _ => super.transformTerm(t)(owner)
              }
            }
        }

        val setupStats = testPathMap.transformStats(setupStatsRaw)(Symbol.spliceOwner)

        val nameExpr = Expr(name)
        val nestedNameTreesExpr = Varargs(nestedNameTrees)
        val names: Expr[UTree[String]] = '{ UTree[String]($nameExpr, $nestedNameTreesExpr: _*)}
        val bodies: Expr[TestCallTree] = testCallTreeExpr(nestedBodyTrees, setupStats)

        (names, bodies)
    }

  private object TestMethod {

    def unapply(using quotes: Quotes)(tree: quotes.reflect.Tree): Option[(Option[String], quotes.reflect.Tree)] =
      import quotes.reflect._
      import quotes.reflect.given

      Option(tree).flatMap{
        case tree: Term =>
          tree match{
            // Somehow this pattern match on the quoted expr doesn't work, so instead
            // match on the tree directly
            //  case '{utest.test($body: Any)} => (None, body.asTerm)
            case Apply(Select(Ident("test"), "apply"), Seq(body)) => Some((None, body))
            case Block(List(Apply(Select(Ident("test"), "apply"), Seq(body))), Literal(UnitConstant())) => Some((None, body))
            case _ =>
              tree.asExpr match{
                case '{utest.test($name: String)($body)} => Some((name.value, body.asTerm))
                case '{utest.test($name: String).-($body)} => Some((name.value, body.asTerm))
                case '{utest.test.-($body)} => Some((None, body.asTerm))
                case expr => None
              }
          }
        case _ => None
      }
  }

  private object Test {
    def unapply(using Quotes)(tree: quotes.reflect.Tree): Option[(Option[String], List[quotes.reflect.Apply], List[quotes.reflect.Statement])] = {
      tree match {
        case TestMethod(name, Stats(nested, stats)) => Some((name, nested, stats))
      }
    }
  }

  private object IsTest {
    def unapply(using Quotes)(tree: quotes.reflect.Tree): Option[quotes.reflect.Apply] =
      import quotes.reflect._
      tree match {
        case t @ TestMethod(_, _) => Some(t.asInstanceOf[Apply])
        case _ => None
      }
  }

  private object Stats {
    def partition(using Quotes)(stats: List[quotes.reflect.Statement]): (List[quotes.reflect.Apply], List[quotes.reflect.Statement]) =
      import quotes.reflect._

      val res = stats.partitionMap[Apply, Statement] {
        case IsTest(test) => Left (test)
        case stmt: Statement => Right(stmt)
      }

      res

    def unapply(using Quotes)(tree: quotes.reflect.Tree): Option[(List[quotes.reflect.Apply], List[quotes.reflect.Statement])] =
      import quotes.reflect._
      tree match {
        case Inlined(_, inlBindings, Stats(tests, testsBindings)) => Some((tests, inlBindings ++ testsBindings))
        case Block(stats, expr) => Some(partition(stats :+ expr))
        case stmt: Statement => Some(partition(stmt :: Nil))
        case _ => None
      }
  }

end TestBuilder
