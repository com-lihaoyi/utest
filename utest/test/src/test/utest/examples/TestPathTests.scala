package test.utest.examples

import utest._

object TestPathTests extends TestSuite{
  val tests = Tests{
    test("testPath"){
      test("foo"){
        assert(implicitly[utest.framework.TestPath].value == Seq("testPath", "foo"))
      }
    }
  }
}