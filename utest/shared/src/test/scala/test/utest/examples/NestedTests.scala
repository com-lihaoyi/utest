package test.utest.examples

import utest._

object NestedTests extends TestSuite{
  val tests =  this{
    val x = 1
    'outer{
      val y = x + 1
      'inner{
        val z = y + 1
        'innerest{
          assert(
            x == 1,
            y == 2,
            z == 3
          )
          (x, y, z)
        }
      }
    }
  }
}