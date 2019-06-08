package test.utest.examples

import utest._

object TestPathTests extends TestSuite{
  val tests = Tests{
    'testPath - {
      'foo - {
        assert(implicitly[utest.framework.TestPath].value == Seq("testPath", "foo"))
      }
    }
  }
}