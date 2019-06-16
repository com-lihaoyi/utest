package test.utest
import utest._
import utest.framework.{Formatter, HTree, Result, Tree}

object DisablePrintTests extends utest.TestSuite{
  override def utestFormatter = new Formatter {
    override def formatSummary(topLevelName: String, results: HTree[String, Result]) = None
    override def formatColor = false
  }

  def tests = Tests{
    test("hello"){
      123
    }
  }
}

