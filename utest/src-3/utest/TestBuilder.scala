package utest

import scala.quoted.{ Type => QType, _ }

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
    val inner =
      if nestedBodyTrees.nonEmpty then Block(setupStats, '{Right(${Expr.ofList(nestedBodyTrees)}.toIndexedSeq)}.asTerm)
      else Block(setupStats.dropRight(1), '{Left(${setupStats.takeRight(1).head.asInstanceOf[Term].asExpr})}.asTerm)
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

    def unapply(using Quotes)(tree: quotes.reflect.Tree): Option[(Option[String], quotes.reflect.Tree)] =
      import quotes.reflect._

      Option(tree).collect { case tree: Term => tree.asExpr }.collect {
        // case q"""utest.`package`.*.-($body)""" => (None, body)
        case '{utest.*.-($body)} => (None, body.asTerm)

        // case q"""$p($value).apply($body)""" if checkLhs(p) => (Some(literalValue(value)), body)
        // case '{($name: TestableString).apply($body)} => (Some(run(name).value), body.asTerm)
        // case '{($sym: scala.Symbol).apply($body)} => (Some(run(sym).name), body.asTerm)

        // case q"""$p($value).-($body)""" if checkLhs(p) => (Some(literalValue(value)), body)
        case '{($name: String).-($body)} => (name.value, body.asTerm)
        // case '{($sym: scala.Symbol).-($body)} => (Some(run(sym).name), body.asTerm)

        // case q"""utest.`package`.test.apply($value).apply($body)""" => (Some(literalValue(value)), body)
        case '{utest.test($name: String)($body)} => (name.value, body.asTerm)

        // case q"""utest.`package`.test.apply($value).-($body)""" => (Some(literalValue(value)), body)
        case '{utest.test($name: String).-($body)} => (name.value, body.asTerm)

        // case q"""utest.`package`.test.-($body)""" => (None, body)
        case '{utest.test.-($body)} => (None, body.asTerm)

        // case q"""utest.`package`.test.apply($body)""" => (None, body)
        case '{utest.test($body: Any)} => (None, body.asTerm)
      }
  }

  private object Test {
    def unapply(using Quotes)(tree: quotes.reflect.Tree): Option[(Option[String], List[quotes.reflect.Apply], List[quotes.reflect.Statement])] = tree match {
      case TestMethod(name, Stats(nested, stats)) => Some((name, nested, stats))
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
      stats.partitionMap[Apply, Statement] {
        case IsTest(test) => Left (test)
        case stmt: Statement => Right(stmt)
      }

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
