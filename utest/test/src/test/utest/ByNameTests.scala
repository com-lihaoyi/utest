package test.utest
import utest._
object ByNameTests extends utest.TestSuite {
  case class X(dummy: Int = 0, x: Int = 0)
  def doAction(action: => Any): Unit = ()
  val tests = Tests{
    doAction {
      X(x = 1)
    }
  }
}