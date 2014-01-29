package microtest
package framework

import scala.reflect.macros.Context
import scala.language.experimental.macros
import microtest.framework.Test


abstract class TestSuite{
  def tests: util.Tree[Test]
}

object TestSuite{
  def apply(expr: Unit): util.Tree[Test] = macro TestSuite.applyImpl

  def applyImpl(c: Context)(expr: c.Expr[Unit]): c.Expr[util.Tree[Test]] = {
    import c.universe._

    def matcher: PartialFunction[Tree, (Tree, Tree)] = {
      case q"""microtest.this.`package`.TestableString($value).-($body)""" => (value, body)
    }

    def recurse(t: Tree): (Tree, Tree) = {
      val b = t match{
        case b: Block => b
        case t => Block(Nil, t)
      }

      val (nested, normal) = b.children.partition(matcher.isDefinedAt)

      val retValueName = c.fresh(newTermName("$ret"))

      val normal2 =
        if (normal.isEmpty || normal.last.isInstanceOf[MemberDefApi]) {
          normal :+ q"val $retValueName = ()"
        }else{
          val (bulk :+ last) = normal
          bulk :+ q"val $retValueName = $last"
        }

      val (names, bodies) = nested.map(matcher).unzip

      val (testTrees, suites) = bodies.map{recurse(_)}.unzip

      val suiteFrags = names.zip(suites).map{
        case (name, suite) => q"$name -> $suite"
      }

      val testTree = q"""
        microtest.framework.TestThunkTree.create{
          ..$normal2
          ($retValueName, Seq(..$testTrees))
        }
      """

      val suite = q"microtest.framework.Test.create(..$suiteFrags)"

      (testTree, suite)
    }

    val (testTree, suite) = recurse(expr.tree.asInstanceOf[Block])
    c.Expr[util.Tree[Test]](c.resetLocalAttrs(q"""$suite(this.getClass.getName, $testTree)"""))
  }
}


