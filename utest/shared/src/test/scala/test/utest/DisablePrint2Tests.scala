package test.utest
import utest._

object DisablePrint2Tests extends utest.TestSuite{
  override def formatColor = false
  def tests = this{
    'hello{
      123
    }
  }
}

