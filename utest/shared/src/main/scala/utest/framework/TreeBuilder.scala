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
//    println("==END==")
//    println(showCode(expr.tree))
    def render(t: c.Tree) = {
      t match{
        case q"scala.Symbol.apply(${Literal(Constant(foo))})" => foo.toString
        case Literal(Constant(foo)) => foo.toString
      }
    }
    def matcher(i: Int): PartialFunction[c.Tree, (String, c.Tree, Int)] = {
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

    def recurse(t: c.Tree, path: Seq[String]): (c.Tree, Seq[c.Tree]) = {
      val b = t match{
        case b: Block => b
        case t => Block(Nil, t)
      }

      val (nested, normal0) = b.children.partition(matcher(0).isDefinedAt)

      val transformer = new Transformer{
        override def transform(t: c.Tree) = {
          t match{
            case q"framework.this.TestPath.synthetic" =>
              c.typeCheck(q"utest.framework.TestPath(Seq(..$path))")
            case _ => super.transform(t)
          }
        }
      }
      val normal = normal0.map(transformer.transform(_))

      val (normal2, last) =
        if (normal.isEmpty
          || normal.last.isInstanceOf[MemberDefApi]
          || nested.contains(b.children.last)) {
          (normal, c.typeCheck(q"()"))
        }else{
          (normal.init, normal.last)
        }

      val (_, thingies) = nested.foldLeft(0 -> Seq[(String, c.Tree, c.Tree)]()) {
        case ((index, trees), nextTree) =>
          val (name, tree2, newIndex) = matcher(index)(nextTree)
          (newIndex, trees :+(name, q"$name", tree2))
      }

      val (names, nameTrees, bodies) = thingies.unzip3

      val (testTrees, suites) =
        thingies
          .map{case (name, tree, body) => recurse(body, path :+ name)}
          .unzip

      val suiteFrags = nameTrees.zip(suites).map{
        case (name, suite) => q"utest.framework.Tree($name, ..$suite)"
      }

      val testTree = c.typeCheck(q"""
        new utest.framework.TestThunkTree({
          ..$normal2
          ${
            if (testTrees.isEmpty) q"Left($last)"
            else q"$last; Right(Seq(..$testTrees))"
          }
        })
      """)


      (testTree, suiteFrags)
    }

    val (testTree, suite) = recurse(expr.tree, Vector())

    val res = q"""
      utest.framework.Test.create(..$suite)(
        this.getClass.getName.replace("$$", ""),
        $testTree
      )"""
//    println("==END==")

//    println(showCode(res))
    // jump through some hoops to avoid using scala.Predef implicits,
    // to make @paulp happy
    c.Expr[framework.Tree[framework.Test]](
      res
    )
  }
}
