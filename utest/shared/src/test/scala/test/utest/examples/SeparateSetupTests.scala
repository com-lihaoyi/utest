package test.utest.examples

import utest._

object SeparateSetupTests extends TestSuite{
  val tests = Tests{
    var x = 0
    'outer1 - {
      x += 1
      'inner1 - {
        x += 2
        assert(x == 3)
        x
      }
      'inner2 - {
        x += 3
        assert(x == 4)
        x
      }
    }
    'outer2 - {
      x += 4
      'inner3 - {
        x += 5
        assert(x == 9)
        x
      }
    }
  }
}