import scala.reflect.ClassTag
import scala.reflect.macros.Context
import utest.framework.{TestTreeSeq, Test}
import utest.asserts._
import utest.util.Tree
import concurrent.duration._
import utest.util.Tree

/**
 * Created by haoyi on 1/24/14.
 */
package object utest {
  implicit val interval = new RetryInterval(100.millis)
  implicit val max = new RetryMax(1.second)
  import language.experimental.macros

  /**
   * Checks that one or more expressions are true; otherwises raises an
   * exception with some debugging info
   */
  def assert(exprs: Boolean*): Unit = macro Asserts.assertProxy
  /**
   * Checks that one or more expressions all become true within a certain
   * period of time. Polls at a regular interval to check this.
   */
  def eventually(exprs: Boolean*): Unit = macro Concurrent.eventuallyProxy
  /**
   * Checks that one or more expressions all remain true within a certain
   * period of time. Polls at a regular interval to check this.
   */
  def continually(exprs: Boolean*): Unit = macro Concurrent.continuallyProxy

  implicit class TestableString(s: String){
    def -(x: => Any) = ???
  }

  implicit def toTestSeq(t: Tree[Test]) = new TestTreeSeq(t)

  val ClassTag = scala.reflect.ClassTag

  def assertMatches[T](t: T)(pf: PartialFunction[T, Unit]): Unit = {
    if (pf.isDefinedAt(t)) pf(t)
    else throw new AssertionError("Matching failed " + t)
  }

  def intercept[T: ClassTag](thunk: => Unit): T = {
    try{
      thunk
      val clsName = implicitly[ClassTag[T]].toString()
      throw new AssertionError(s"Thunk failed to raise $clsName!")
    } catch { case e: T => e }
  }

  def desugar(a: Any): String = macro desugarImpl

  def desugarImpl(c: Context)(a: c.Expr[Any]) = {
    import c.universe._

    val s = show(a.tree)
    c.Expr(
      Literal(Constant(s))
    )
  }
}

