package utest

import scala.quoted.{ Type => QType, _ }

import utest.framework.{TestCallTree, Tree => UTree, TestPath }


class TestBuilder[QCtx <: QuoteContext & Singleton](ctx: QCtx) extends TestBuilderExtractors[QCtx](using ctx) {
  import qc.tasty.{ Tree => TasTree, _ }

  def buildTestsTrees(tests: List[Apply], path: Seq[String]): (List[Expr[UTree[String]]], List[Expr[TestCallTree]]) =
    if tests.isEmpty then Nil -> Nil else tests.zipWithIndex.foldLeft((List.empty[Expr[UTree[String]]], List.empty[Expr[TestCallTree]])) {
      case ((namesForest, callsForest), (nextTest, id)) =>
        val (name, body) = processTest(nextTest, path, id)
        (namesForest :+ name, callsForest :+ body)
    }

  def TestCallTreeExpr(nestedBodyTrees: List[Expr[TestCallTree]], setupStats: List[Statement]): Expr[TestCallTree] = '{TestCallTree { ${(
    if (nestedBodyTrees.nonEmpty)
      Block(setupStats, '{Right(${Expr.ofList(nestedBodyTrees)}.toIndexedSeq)}.unseal)
    else
      Block(setupStats.dropRight(1), '{Left(${setupStats.takeRight(1).head.asInstanceOf[Term].seal})}.unseal)
    ).seal.cast[Either[Any, IndexedSeq[TestCallTree]]]
  }}}


  def processTest(test: Apply, pathOld: Seq[String], index: Int): (Expr[UTree[String]], Expr[TestCallTree]) = test match {
    case Test(nameOpt, nestedTests, setupStatsRaw) =>
      val name = nameOpt.getOrElse(index.toString)
      val path = pathOld :+ name
      val (nestedNameTrees, nestedBodyTrees) = buildTestsTrees(nestedTests, path)

      object testPathMap extends TreeMap {
        override def transformTerm(t: Term)(implicit ctx: Context): Term =
          t.tpe.widen match {
            case _: MethodType | _: PolyType => super.transformTerm(t)
            case _ => t.seal match {
              case '{TestPath.synthetic} => '{TestPath(${Expr(path)})}.unseal
              case _ => super.transformTerm(t)
            }
          }
      }

      val setupStats = testPathMap.transformStats(setupStatsRaw)

      val names: Expr[UTree[String]] = '{UTree[String](${Expr(name)}, ${Expr.ofList(nestedNameTrees)}: _*)}
      val bodies: Expr[TestCallTree] = TestCallTreeExpr(nestedBodyTrees, setupStats)

      (names, bodies)
  }

  def processTests(body: Term): Expr[Tests] =
    body match {
      case Stats(tests, setupStats) =>
        val (nestedNameTrees, nestedBodyTrees) = buildTestsTrees(tests, Vector())
        '{Tests(UTree[String](
            "", ${Expr.ofSeq(nestedNameTrees)}: _*)
          , ${TestCallTreeExpr(nestedBodyTrees, setupStats)})}
    }
}

trait TestBuilderExtractors[QCtx <: QuoteContext & Singleton](using val qc: QCtx) {
  import qc.tasty._

  object TestMethod {

    def unapply(tree: Tree): Option[(Option[String], Tree)] =
      Option(tree).collect { case tree: Term => tree.seal }.collect {
        // case q"""utest.`package`.*.-($body)""" => (None, body)
        case '{utest.*.-($body)} => (None, body.unseal)

        // case q"""$p($value).apply($body)""" if checkLhs(p) => (Some(literalValue(value)), body)
        // case '{($name: TestableString).apply($body)} => (Some(run(name).value), body.unseal)
        // case '{($sym: scala.Symbol).apply($body)} => (Some(run(sym).name), body.unseal)

        // case q"""$p($value).-($body)""" if checkLhs(p) => (Some(literalValue(value)), body)
        case '{($name: String).-($body)} => (name.unlift, body.unseal)
        // case '{($sym: scala.Symbol).-($body)} => (Some(run(sym).name), body.unseal)

        // case q"""utest.`package`.test.apply($value).apply($body)""" => (Some(literalValue(value)), body)
        case '{utest.test($name: String)($body)} => (name.unlift, body.unseal)

        // case q"""utest.`package`.test.apply($value).-($body)""" => (Some(literalValue(value)), body)
        case '{utest.test($name: String).-($body)} => (name.unlift, body.unseal)

        // case q"""utest.`package`.test.-($body)""" => (None, body)
        case '{utest.test.-($body)} => (None, body.unseal)

        // case q"""utest.`package`.test.apply($body)""" => (None, body)
        case '{utest.test($body: Any)} => (None, body.unseal)
      }
  }

  object Test {
    def unapply(tree: Tree): Option[(Option[String], List[Apply], List[Statement])] = tree match {
      case TestMethod(name, Stats(nested, stats)) => Some((name, nested, stats))
    }
  }

  object IsTest {
    def unapply(tree: Tree): Option[Apply] = tree match {
      case t@TestMethod(_, _) => Some(t.asInstanceOf[Apply])
      case _ => None
    }
  }

  object Stats {
    def partition(stats: List[Statement]): (List[Apply], List[Statement]) =
      stats.partitionMap[Apply, Statement] {
        case IsTest(test) => Left (test)
        case stmt: Statement => Right(stmt)
        case stmt: Import => Right(stmt)
      }

    def unapply(tree: Tree): Option[(List[Apply], List[Statement])] = tree match {
      case Inlined(_, inlBindings, Stats(tests, testsBindings)) => Some((tests, inlBindings ++ testsBindings))
      case Block(stats, expr) => Some(partition(stats :+ expr))
      case stmt: Statement => Some(partition(stmt :: Nil))
      case _ => None
    }
  }
}

