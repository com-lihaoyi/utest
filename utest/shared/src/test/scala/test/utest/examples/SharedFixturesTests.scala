package test.utest.examples

import utest._

object SharedFixturesTests extends TestSuite{
  var x = 0
  val tests = this{
    'A{
      x += 1
      'X{
        x += 2
        assert(x == 4)
        x
      }
      'Y{
        x += 3
        assert(x == 8)
        x
      }
    }
    'B{
      x += 4
      'Z{
        x += 5
        assert(x == 21)
        x
      }
    }
  }
}