package test.utest
import utest._
case class X(dummy: Int = 0, x: Int = 0)
object ByNameTests extends utest.TestSuite {
  def doAction(action: => Any): Unit = ()
  val tests = this{
      doAction {
        X(x = 1)
      }
  }
}