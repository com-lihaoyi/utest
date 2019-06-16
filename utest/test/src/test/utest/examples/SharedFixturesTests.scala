package test.utest.examples

import utest._

object SharedFixturesTests extends TestSuite{
  var x = 0
  val tests = Tests{
    test("outer1"){
      x += 1
      test("inner1"){
        x += 2
        assert(x == 3) // += 1, += 2
        x
      }
      test("inner2"){
        x += 3
        assert(x == 7) // += 1, += 2, += 1, += 3
        x
      }
    }
    test("outer2"){
      x += 4
      test("inner3"){
        x += 5
        assert(x == 16) // += 1, += 2, += 1, += 3, += 4, += 5
        x
      }
    }
  }
}