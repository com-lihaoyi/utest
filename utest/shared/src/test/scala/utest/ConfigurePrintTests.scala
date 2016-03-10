package utest

import utest.framework.{Result, Tree}

object ConfigurePrintTests extends TestSuite{
  override def formatSingle(path: Seq[String], res: utest.framework.Result) = None
  override def format(results: Tree[Result]) = None

  def tests = this{
    'hello{
      123
    }
  }
}

