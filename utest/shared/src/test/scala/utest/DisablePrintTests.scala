package utest

import utest.framework.{Result, Tree}

object DisablePrintTests extends TestSuite{
  override def format(results: Tree[Result]) = None
  override def formatColor = false
  def tests = this{
    'hello{
      123
    }
  }
}

