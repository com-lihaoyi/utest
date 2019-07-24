package utest

import scala.quoted.{ Type => QType, _ }
import scala.tasty._

import utest.framework.{TestCallTree, Tree => UTree }


class TestBuilder given (val qc: QuoteContext) given Toolbox extends TestBuilderExtractors {
  import qc.tasty.{ Tree => TasTree, _ }

  def buildTestsTrees(tests: List[Apply]): (List[Expr[UTree[String]]], List[Expr[TestCallTree]]) = tests match {
    case t :: ts =>
      val (name, body) = processTest(t)
      val (names, bodies) = buildTestsTrees(ts)
      (name :: names, body :: bodies)

    case Nil => (Nil, Nil)
  }

  def TestCallTreeExpr(nestedBodyTrees: List[Expr[TestCallTree]], setupStats: List[Statement]): Expr[TestCallTree] = '{TestCallTree { ${(
    if (nestedBodyTrees.nonEmpty)
      Block(setupStats, '{Right(${nestedBodyTrees.toExprOfList})}.unseal)
    else
      Block(setupStats.dropRight(1), '{Left(${setupStats.takeRight(1).head.asInstanceOf[Term].seal})}.unseal)
    ).seal.cast[Either[Any, IndexedSeq[TestCallTree]]]
  }}}


  def processTest(test: Apply): (Expr[UTree[String]], Expr[TestCallTree]) = test match {
    case Test(name, nestedTests, setupStats) =>
      val (nestedNameTrees, nestedBodyTrees) = buildTestsTrees(nestedTests)

      val names: Expr[UTree[String]] = '{UTree[String](${name.toExpr}, ${nestedNameTrees.toExprOfList}: _*)}
      val bodies: Expr[TestCallTree] = TestCallTreeExpr(nestedBodyTrees, setupStats)

      (names, bodies)
  }

  def processTests(body: Term): Expr[Tests] = body.underlyingArgument match {
      case Stats(tests, setupStats) =>
        val (nestedNameTrees, nestedBodyTrees) = buildTestsTrees(tests)
        '{Tests(UTree[String](
            "<root>", ${nestedNameTrees.toExprOfList}: _*)
          , ${TestCallTreeExpr(nestedBodyTrees, setupStats)})}
    }
}

trait TestBuilderExtractors {
  val qc: QuoteContext
  import qc.tasty._

  object Test {
    def unapply(tree: Tree): Option[(String, List[Apply], List[Statement])] = tree match {
      case Apply(
        TestName(testName)
      , Stats(nestedTests, setupStats) :: Nil) => Some((testName, nestedTests, setupStats))
      case _ => None
    }
  }

  object TestName {
    def unapply(tree: Tree): Option[String] = tree match {
      // '{ test($name)($body) }
      case Select(Apply(Select(Ident("test"),"apply"),List(Literal(Constant(name: String)))),"apply") => Some(name)
      case _ => None
    }
  }

  object IsTest {
    def unapply(tree: Tree): Option[Apply] = tree match {
      // case t@Apply(Apply(Ident("test"), List(Literal(Constant(_)))), _) => Some(t.asInstanceOf[Apply])
      case t@Apply(TestName(_), _) => Some(t.asInstanceOf[Apply])
      case _ => None
    }
  }

  object Stats {
    def (lst: List[A]) partitionMap[A, B, C] (f: A => Either[B, C]): (List[B], List[C]) =
      lst.foldLeft((Nil: List[B], Nil: List[C])) { case ((bs, cs), next) => f(next) match {
        case Left (b) => (bs :+ b, cs)
        case Right(c) => (bs, cs :+ c)
      }}

    def partition(stats: List[Statement]): (List[Apply], List[Statement]) =
      stats.partitionMap[Statement, Apply, Statement] {
        case IsTest     (test) => Left (test)
        case IsStatement(stmt) => Right(stmt)
        case IsImport   (stmt) => Right(stmt)
      }

    def unapply(tree: Tree): Option[(List[Apply], List[Statement])] = tree match {
      case Block(stats, expr) => Some(partition(stats :+ expr))
      case IsStatement(stmt) => Some(partition(stmt :: Nil))
      case _ => None
    }
  }
}

delegate for Toolbox = Toolbox.make(getClass.getClassLoader)
delegate for TestBuilder given QuoteContext, Toolbox = new TestBuilder