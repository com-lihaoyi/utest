package test.utest.examples

import utest._

object NestedTests extends TestSuite{
  val tests =  Tests{
    val x = 1
    'outer1 - {
      val y = x + 1
      'inner1 - {
        val z = y + 1
        'innerest - {
          assert(
            x == 1,
            y == 2,
            z == 3
          )
          (x, y, z)
        }
      }
    }
    'outer2 - {
      'inner2 - {
        assert(4 > 3)
      }
      'inner3 - {
        assert(5 > 4)
      }
    }
  }
}