package test.utest.examples

import utest._

object SharedSetupTests extends TestSuite{
  val tests = this{
    var x = 0
    'A{
      x += 1
      'X{
        x += 2
        assert(x == 3)
        x
      }
      'Y{
        x += 3
        assert(x == 4)
        x
      }
    }
    'B{
      x += 4
      'Z{
        x += 5
        assert(x == 9)
        x
      }
    }
  }
}