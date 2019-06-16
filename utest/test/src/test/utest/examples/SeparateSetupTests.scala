package test.utest.examples

import utest._

object SeparateSetupTests extends TestSuite{
  val tests = Tests{
    var x = 0
    test("outer1"){
      x += 1
      test("inner1"){
        x += 2
        assert(x == 3) // += 1, += 2
        x
      }
      test("inner2"){
        x += 3
        assert(x == 4) // += 1, += 3
        x
      }
    }
    test("outer2"){
      x += 4
      test("inner3"){
        x += 5
        assert(x == 9) // += 4, += 5
        x
      }
    }
  }
}
