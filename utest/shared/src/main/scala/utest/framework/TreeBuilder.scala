package utest.framework

import utest.framework

import scala.reflect.macros._

/**
  * Created by haoyi on 3/11/16.
  */
object TreeBuilder {
  /**
    * Raise an exception if a test is nested badly within a `TestSuite{ ... }`
    * block.
    */
  def dieInAFire(testName: String) = {
    throw new IllegalArgumentException(s"Test nested badly: $testName")
  }

  def applyImpl(c: Context)(expr: c.Expr[Unit]): c.Expr[framework.Tree[framework.Test]] = {
    import c.universe._

    def matcher(i: Int): PartialFunction[Tree, (String, Tree, Int)] = {
      case q"""utest.this.`package`.TestableSymbol($value).apply($body)""" => ("foo", body, i)
      case q"""utest.`package`.TestableSymbol($value).apply($body)""" => ("foo", body, i)
      case q"""utest.this.`package`.TestableSymbol($value).-($body)""" => ("foo", body, i)
      case q"""utest.`package`.TestableSymbol($value).-($body)""" => ("foo", body, i)
    }

    def recurse(t: Tree, path: Seq[String]): (Tree, Tree) = {
      val b = t match{
        case b: Block => b
        case t => Block(Nil, t)
      }

      val (nested, normal) = b.children.partition(matcher(0).isDefinedAt)

      val (_, thingies) = nested.foldLeft(0 -> Seq[(String, Tree, Tree)]()) { case ((index, trees), nextTree) =>
        val (name, tree2, newIndex) = matcher(index)(nextTree)
        (newIndex, trees :+(name, q"$name", tree2))
      }

      val (names, nameTrees, bodies) = thingies.unzip3

      val (testTrees, suites) =
        thingies
          .map{case (name, tree, body) => recurse(body, path :+ name)}
          .unzip

      val suiteFrags = nameTrees.zip(suites).map{
        case (name, suite) => q"$name -> $suite"
      }


      val testTree = c.typeCheck(q"""
        new utest.framework.TestThunkTree(({
          ..$normal

        }, Seq(..$testTrees)))
      """)

      val suite = q"utest.framework.Test.create(..$suiteFrags)"

      (testTree, suite)
    }

    val (testTree, suite) = recurse(expr.tree, Vector())

    val res = q"""$suite(this.getClass.getName.replace("$$", ""), $testTree)"""
//    println("==END==")
//    println(showCode(res))
    // jump through some hoops to avoid using scala.Predef implicits,
    // to make @paulp happy
    c.Expr[framework.Tree[framework.Test]](
      res
    )
  }
}
