package test.utest.examples


import utest._

object NestedTests extends TestSuite{
  val tests =  Tests{
    val x = 1
    test("outer1"){
      val y = x + 1

      test("inner1"){
        assert(x == 1, y == 2)
        (x, y)
      }
      test("inner2"){
        val z = y + 1
        assert(z == 3)
      }
    }
    test("outer2"){
      test("inner3"){
        assert(x == 1)
      }
    }
  }
}