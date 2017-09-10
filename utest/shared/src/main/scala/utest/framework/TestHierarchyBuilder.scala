package utest.framework

import utest.framework

import scala.collection.mutable
import scala.reflect.macros._

/**
  * Created by haoyi on 3/11/16.
  */
object TestHierarchyBuilder {
  /**
    * Raise an exception if a test is nested badly within a `TestSuite{ ... }`
    * block.
    */
  def dieInAFire(testName: String) = {
    throw new IllegalArgumentException(s"Test nested badly: $testName")
  }

  def applyImpl(c: Context)(expr: c.Expr[Unit]): c.Expr[TestHierarchy] = {
    import c.universe._

    def literalValue(t: c.Tree) = {
      t match{
        case q"scala.Symbol.apply(${Literal(Constant(foo))})" => foo.toString
        case Literal(Constant(foo)) => foo.toString
      }
    }

    def checkLhs(prefix: c.Tree) = prefix match{
      case q"utest.this.`package`.TestableString" => true
      case q"utest.`package`.TestableString" => true
      case q"utest.this.`package`.TestableSymbol" => true
      case q"utest.`package`.TestableSymbol" => true
      case _ => false
    }
    def matcher(i: Int): PartialFunction[c.Tree, (String, c.Tree, Int)] = {
      // Special case for *
      case q"""utest.this.`package`.*.-($body)""" => (i.toString, body, i + 1)
      case q"""utest.`package`.*.-($body)""" => (i.toString, body, i + 1)
      case q"""$p($value).apply($body)""" if checkLhs(p) => (literalValue(value), body, i)
      case q"""$p($value).-($body)""" if checkLhs(p) => (literalValue(value), body, i)
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
              c.typeCheck(q"_root_.utest.framework.TestPath(_root_.scala.Array(..$path))")
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

      val (names, bodies) = {
        var index = 0
        val names = mutable.Buffer.empty[String]
        val bodies = mutable.Buffer.empty[c.Tree]
        for(inner <- nested){
          val (name, tree2, newIndex) = matcher(index)(inner)
          names.append(name)
          bodies.append(tree2)
          index = newIndex
        }
        (names, bodies)
      }

      val (childCallTrees, childNameTrees) =
        names.zip(bodies)
          .map{case (name, body) => recurse(body, path :+ name)}
          .unzip

      val nameTree = names.zip(childNameTrees).map{
        case (name, suite) => q"_root_.utest.framework.Tree($name, ..$suite)"
      }

      val callTree = c.typeCheck(q"""
        new _root_.utest.framework.TestCallTree({
          ..$normal2
          ${
            if (childCallTrees.isEmpty) q"_root_.scala.Left($last)"
            else q"$last; _root_.scala.Right(Array(..$childCallTrees))"
          }
        })
      """)

      (callTree, nameTree)
    }

    val (callTree, nameTree) = recurse(expr.tree, Vector())

    val res = q"""
      _root_.utest.framework.TestHierarchy(
        _root_.utest.framework.Tree(
          this.getClass.getName.replace("$$", ""),
          ..$nameTree
        ),
        $callTree
      )"""

    c.Expr[TestHierarchy](res)
  }
}
