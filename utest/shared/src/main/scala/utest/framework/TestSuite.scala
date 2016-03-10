package utest
package framework
import acyclic.file
import scala.reflect.macros.Context
import scala.language.experimental.macros

import scala.scalajs.js.annotation.JSExportDescendentObjects

/**
 * Marker class used to mark an `object` as something containing tests. Used
 * for test-discovery by SBT.
 */
@JSExportDescendentObjects
abstract class TestSuite extends TestSuiteMacro{
  /**
   * The tests within this `object`.
   */
  def tests: framework.Tree[Test]
}
trait TestSuiteMacro{


  /**
    * Macro to demarcate a `Tree[Test]`.
    */
  def apply(expr: Unit): framework.Tree[Test] = macro TestSuite.applyImpl
}
object TestSuite extends TestSuiteMacro{
  /**
   * Raise an exception if a test is nested badly within a `TestSuite{ ... }`
   * block.
   */
  def dieInAFire(testName: String) = {
    throw new IllegalArgumentException(s"Test nested badly: $testName")
  }

  def applyImpl(c: Context)(expr: c.Expr[Unit]): c.Expr[framework.Tree[Test]] = {
    import c.universe._

    def matcher(i: Int): PartialFunction[Tree, (Tree, Tree, Int)] = {
      // Special case for *
      case q"""utest.this.`package`.*.-($body)""" => (q"${i.toString}", body, i + 1)
      case q"""utest.`package`.*.-($body)""" => (q"${i.toString}", body, i + 1)

      // Strings using -
      case q"""utest.this.`package`.TestableString($value).-($body)""" => (value, body, i)
      case q"""utest.`package`.TestableString($value).-($body)""" => (value, body, i)

      // Symbols using - or apply
      case q"""utest.this.`package`.TestableSymbol($value).apply($body)""" => (q"$value.name", body, i)
      case q"""utest.`package`.TestableSymbol($value).apply($body)""" => (q"$value.name", body, i)
      case q"""utest.this.`package`.TestableSymbol($value).-($body)""" => (q"$value.name", body, i)
      case q"""utest.`package`.TestableSymbol($value).-($body)""" => (q"$value.name", body, i)
    }

    def recurse(t: Tree): (Tree, Tree) = {
      val b = t match{
        case b: Block => b
        case t => Block(Nil, t)
      }

      val (nested, normal) = b.children.partition(matcher(0).isDefinedAt)

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

      val thingies = nested.foldLeft(0 -> Seq[(Tree, Tree)]()) { case ((index, trees), nextTree) =>
        val (tree1, tree2, newIndex) = matcher(index)(nextTree)
        (newIndex, trees :+(tree1, tree2))
      }

      val (names, bodies) = thingies._2.unzip

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
        found = found.orElse(matcher(0).lift(t).map(_._1))
        super.transform(t)
      }
    }
    testTree.foreach(transformer.transform(_))

    found match{
      case Some(tree) =>
        c.Expr[framework.Tree[Test]](q"""
          throw new java.lang.IllegalArgumentException("Test [" + $tree + "] nested badly. Tests must be nested directly underneath their parents and can not be placed within blocks.")
        """)

      case None =>
        // jump through some hoops to avoid using scala.Predef implicits,
        // to make @paulp happy
        c.Expr[framework.Tree[Test]](
          q"""$suite(this.getClass.getName.replace("$$", ""), $testTree)"""
        )
    }
  }
}


