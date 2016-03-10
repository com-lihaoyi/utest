package utest

import utest.framework.{Result, Tree}

object DisablePrint2Tests extends TestSuite{
  override def formatSingle(path: Seq[String], res: utest.framework.Result) = None
  override def formatColor = false
  def tests = this{
    'hello{
      123
    }
  }
}

