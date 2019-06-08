package test.utest.examples


import utest._

object NestedTests extends TestSuite{
  val tests =  Tests{
    val x = 1
    'outer1 - {
      val y = x + 1

      'inner1 - {
        assert(x == 1, y == 2)
        (x, y)
      }
      'inner2 - {
        val z = y + 1
        assert(z == 3)
      }
    }
    'outer2 - {
      'inner3 - {
        assert(x == 1)
      }
    }
  }
}