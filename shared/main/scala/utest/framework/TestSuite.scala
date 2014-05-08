package utest
package framework

import scala.reflect.macros.Context
import scala.language.experimental.macros
import utest.framework.Test

/**
 * Marker class used to mark an `object` as something containing tests. Used
 * for test-discovery by SBT.
 */
abstract class TestSuite extends PlatformShims.Test{
  /**
   * The tests within this `object`.
   */
  def tests: util.Tree[Test]
}

object TestSuite{
  /**
   * Macro to demarcate a `Tree[Test]`.
   */
  def apply(expr: Unit): util.Tree[Test] = macro TestSuite.applyImpl

  /**
   * Raise an exception if a test is nested badly within a `TestSuite{ ... }`
   * block.
   */
  def dieInAFire(testName: String) = {
    throw new IllegalArgumentException(s"Test nested badly: $testName")
  }

  def applyImpl(c: Context)(expr: c.Expr[Unit]): c.Expr[util.Tree[Test]] = {
    import c.universe._

    def matcher: PartialFunction[Tree, (Tree, Tree)] = {
      case q"""utest.this.`package`.TestableString($value).-($body)""" => (value, body)
      case q"""utest.`package`.TestableString($value).-($body)""" => (value, body)
      case q"""utest.this.`package`.TestableSymbol($value).apply($body)""" => (q"$value.name", body)
      case q"""utest.`package`.TestableSymbol($value).apply($body)""" => (q"$value.name", body)
      case q"""utest.this.`package`.TestableSymbol($value).-($body)""" => (q"$value.name", body)
      case q"""utest.`package`.TestableSymbol($value).-($body)""" => (q"$value.name", body)
    }

    def recurse(t: Tree): (Tree, Tree) = {
      val b = t match{
        case b: Block => b
        case t => Block(Nil, t)
      }

      val (nested, normal) = b.children.partition(matcher.isDefinedAt)

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

      val (names, bodies) = nested.map(matcher).unzip

      val (testTrees, suites) = bodies.map{recurse(_)}.unzip

      val suiteFrags = names.zip(suites).map{
        case (name, suite) => q"$name -> $suite"
      }


      val testTree = q"""
        utest.framework.TestThunkTree.create{
          ..$normal2
          ($retValueName, Seq(..$testTrees))
        }
      """

      val suite = q"utest.framework.Test.create(..$suiteFrags)"

      (testTree, suite)
    }

    val (testTree, suite) = recurse(expr.tree)
    var found: Option[Tree] = None
    val transformer = new Transformer{
      override def transform(t: Tree) = {
        //          println("transforming " + t)
        found = found.orElse(matcher.lift(t).map(_._1))
        super.transform(t)
      }
    }
    testTree.foreach(transformer.transform(_))

    found match{
      case Some(tree) =>
        c.Expr[util.Tree[Test]](q"""
          throw new java.lang.IllegalArgumentException("Test [" + $tree + "] nested badly. Tests must be nested directly underneath their parents and can not be placed within blocks.")
        """)

      case None =>
        // jump through some hoops to avoid using scala.Predef implicits,
        // to make @paulp happy
        c.Expr[util.Tree[Test]](
          q"""$suite(this.getClass.getName.replace("$$", ""), $testTree)"""
        )
    }
  }
}


