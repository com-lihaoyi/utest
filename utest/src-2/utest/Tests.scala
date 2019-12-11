package utest

import utest.framework.{TestCallTree, Tree}
import scala.language.experimental.macros
import scala.collection.mutable
import scala.reflect.macros._

trait TestsVersionSpecific {
  import TestsVersionSpecific._
  def apply(expr: Unit): Tests = macro Builder.applyImpl
}

object TestsVersionSpecific {
  object Builder {
    /**
      * Raise an exception if a test is nested badly within a `TestSuite{ ... }`
      * block.
      */
    def dieInAFire(testName: String) = {
      throw new IllegalArgumentException(s"Test nested badly: $testName")
    }

    def applyImpl(c: Context)(expr: c.Expr[Unit]): c.Expr[Tests] = {
      import c.universe._

      def literalValue(t: c.Tree): String = {
        t match{
          case q"scala.Symbol.apply(${Literal(Constant(foo))})" => foo.toString
          case Literal(Constant(foo: String)) => foo
          case Literal(Constant(foo: scala.Symbol)) => foo.name
        }
      }

      def checkLhs(prefix: c.Tree) = prefix match{
        case q"utest.this.`package`.TestableString" => true
        case q"utest.`package`.TestableString" => true
        case q"utest.this.`package`.TestableSymbol" => true
        case q"utest.`package`.TestableSymbol" => true
        case _ => false
      }
      def matcher(i: Int): PartialFunction[c.Tree, (Option[String], c.Tree)] = {
        // Special case for *
        case q"""utest.this.`package`.*.-($body)""" => (None, body)
        case q"""utest.`package`.*.-($body)""" => (None, body)

        case q"""$p($value).apply($body)""" if checkLhs(p) => (Some(literalValue(value)), body)
        case q"""$p($value).-($body)""" if checkLhs(p) => (Some(literalValue(value)), body)

        case q"""utest.this.`package`.test.apply($value).apply($body)""" => (Some(literalValue(value)), body)
        case q"""utest.`package`.test.apply($value).apply($body)""" => (Some(literalValue(value)), body)

        case q"""utest.this.`package`.test.apply($value).-($body)""" => (Some(literalValue(value)), body)
        case q"""utest.`package`.test.apply($value).-($body)""" => (Some(literalValue(value)), body)

        case q"""utest.this.`package`.test.-($body)""" => (None, body)
        case q"""utest.`package`.test.-($body)""" => (None, body)

        case q"""utest.this.`package`.test.apply($body)""" => (None, body)
        case q"""utest.`package`.test.apply($body)""" => (None, body)
      }

      def recurse(t: c.Tree, path: Seq[String]): (c.Tree, collection.Seq[c.Tree]) = {
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
            (normal, q"()")
          }else{
            (normal.init, normal.last)
          }

        val (names, bodies) = {
          var index = 0
          val names = mutable.Buffer.empty[String]
          val bodies = mutable.Buffer.empty[c.Tree]
          for(inner <- nested){
            val (nameOpt, tree2) = matcher(index)(inner)
            nameOpt match{
              case Some(name) => names.append(name)
              case None =>
                names.append(index.toString)
                index += 1
            }
            bodies.append(tree2)

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

        val callTree = q"""
        new _root_.utest.framework.TestCallTree({
          ..$normal2
          ${
          if (childCallTrees.isEmpty) q"_root_.scala.Left($last)"
          else q"$last; _root_.scala.Right(_root_.scala.collection.immutable.IndexedSeq(..$childCallTrees))"
        }
        })
      """

        (callTree, nameTree)
      }

      val (callTree, nameTree) = recurse(expr.tree, Vector())

      val res = q"""
      _root_.utest.Tests(
        _root_.utest.framework.Tree(
          "",
          ..$nameTree
        ),
        $callTree
      )"""

      c.Expr[Tests](res)
    }
  }

}

