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

    def render(t: Tree) = {
      t match{
        case q"scala.Symbol.apply(${Literal(Constant(foo))})" => foo.toString
        case Literal(Constant(foo)) => foo.toString
      }
    }
    def matcher(i: Int): PartialFunction[Tree, (String, Tree, Int)] = {
      // Special case for *
      case q"""utest.this.`package`.*.-($body)""" => (i.toString, body, i + 1)
      case q"""utest.`package`.*.-($body)""" => (i.toString, body, i + 1)

      // Strings using -
      case q"""utest.this.`package`.TestableString($value).-($body)""" => (render(value), body, i)
      case q"""utest.`package`.TestableString($value).-($body)""" => (render(value), body, i)

      // Symbols using - or apply
      case q"""utest.this.`package`.TestableSymbol($value).apply($body)""" => (render(value), body, i)
      case q"""utest.`package`.TestableSymbol($value).apply($body)""" => (render(value), body, i)
      case q"""utest.this.`package`.TestableSymbol($value).-($body)""" => (render(value), body, i)
      case q"""utest.`package`.TestableSymbol($value).-($body)""" => (render(value), body, i)
    }

    def recurse(t: Tree, path: Seq[String]): (Tree, Tree) = {
      val b = t match{
        case b: Block => b
        case t => Block(Nil, t)
      }

      val (nested, normal0) = b.children.partition(matcher(0).isDefinedAt)

      val transformer = new Transformer{
        override def transform(t: Tree) = {
          t match{
            case q"framework.this.TestPath.synthetic" =>
              c.typeCheck(q"utest.framework.TestPath(Seq(..$path))")
            case _ => super.transform(t)
          }
        }
      }
      val normal = normal0.map(transformer.transform(_))
      val retValueName = c.fresh(newTermName("$ret"))

      val normal2 =
        if (normal.isEmpty
          || normal.last.isInstanceOf[MemberDefApi]
          || nested.contains(b.children.last)) {
          normal :+ q"val $retValueName = ()"
        }else{
          val (bulk :+ last) = normal
          bulk :+ q"val $retValueName = $last"
        }

      val thingies = nested.foldLeft(0 -> Seq[(String, Tree, Tree)]()) { case ((index, trees), nextTree) =>
        val (name, tree2, newIndex) = matcher(index)(nextTree)
        (newIndex, trees :+(name, q"$name", tree2))
      }

      val (names, nameTrees, bodies) = thingies._2.unzip3

      val (testTrees, suites) =
        thingies._2
          .map{case (name, tree, body) => recurse(body, path :+ name)}
          .unzip

      val suiteFrags = nameTrees.zip(suites).map{
        case (name, suite) => q"$name -> $suite"
      }


      val testTree = q"""
        new utest.framework.TestThunkTree({
          ..$normal2
          ($retValueName, Seq(..$testTrees))
        })
      """

      val suite = q"utest.framework.Test.create(..$suiteFrags)"

      (testTree, suite)
    }

    val (testTree, suite) = recurse(expr.tree, Vector())
    // jump through some hoops to avoid using scala.Predef implicits,
    // to make @paulp happy
    c.Expr[framework.Tree[framework.Test]](
      q"""$suite(this.getClass.getName.replace("$$", ""), $testTree)"""
    )
  }
}
