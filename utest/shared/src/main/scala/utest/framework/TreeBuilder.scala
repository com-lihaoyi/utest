package utest.framework

import utest.framework

import scala.reflect.macros._

object TreeBuilder {

  def applyImpl(c: Context)(expr: c.Expr[Unit]): c.Expr[framework.Tree[framework.Test]] = {
    import c.universe._

    def matcher(i: Int): PartialFunction[Tree, (String, Tree, Int)] = {
      case q"""utest.`package`.TestableSymbol($value).apply($body)""" => ("foo", body, i)
    }

    def recurse(t: Tree): (Tree, Tree) = {


      val (nested, normal) = t.children.partition(matcher(0).isDefinedAt)

      val (_, thingies) = nested.foldLeft(0 -> Seq[(Tree)]()) {
        case ((index, trees), nextTree) =>
          val (name, tree2, newIndex) = matcher(index)(nextTree)
          (newIndex, trees :+ tree2)
      }

      val (testTrees, suites) =
        thingies.map{case body => recurse(body)}.unzip

      val testTree = c.typeCheck(q"""
        new utest.framework.TestThunkTree(({
          ..$normal
        }, Seq(..$testTrees)))
      """)

      val suite = q"utest.framework.Test.create(..$suites)"

      (testTree, suite)
    }

    val (testTree, suite) = recurse(expr.tree)



    // jump through some hoops to avoid using scala.Predef implicits,
    // to make @paulp happy
    println(showCode(q"""$suite($testTree)"""))
    c.Expr[framework.Tree[framework.Test]](q"""$suite($testTree)""")
  }
}
