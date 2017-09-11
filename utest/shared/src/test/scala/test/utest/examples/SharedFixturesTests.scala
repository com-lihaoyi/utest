package test.utest.examples

import utest._

object SharedFixturesTests extends TestSuite{
  var x = 0
  val tests = this{
    'A{
      x += 1
      'X{
        x += 2
        assert(x == 3)
        x
      }
      'Y{
        x += 3
        assert(x == 7)
        x
      }
    }
    'B{
      x += 4
      'Z{
        x += 5
        assert(x == 16)
        x
      }
    }
  }
}