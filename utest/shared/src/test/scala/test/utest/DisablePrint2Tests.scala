package test.utest
import utest._

object DisablePrint2Tests extends utest.TestSuite{
  override def formatSingle(path: Seq[String], res: utest.framework.Result) = None
  override def formatColor = false
  def tests = Tests{
    'hello - {
      123
    }
  }
}

