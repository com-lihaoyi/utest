package test.utest
import utest._
import utest.framework.Formatter

object DisablePrint2Tests extends utest.TestSuite{
  override def utestFormatter = new Formatter {
    override def formatSingle(path: Seq[String], res: utest.framework.Result) = None
    override def formatColor = false
  }
  def tests = Tests{
    test("hello"){
      123
    }
  }
}

