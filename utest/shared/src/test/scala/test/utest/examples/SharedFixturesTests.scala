package test.utest.examples

import utest._

object SharedFixturesTests extends TestSuite{
  var x = 0
  val tests = this{
    'outer1 - {
      x += 1
      'inner1 - {
        x += 2
        assert(x == 3)
        x
      }
      'inner2 - {
        x += 3
        assert(x == 7)
        x
      }
    }
    'outer2{
      x += 4
      'inner3 - {
        x += 5
        assert(x == 16)
        x
      }
    }
  }
}