package test.utest.examples

import utest._
object BeforeAfterEachTest extends TestSuite{
  var x = 0
  override def beforeEach = {
    println(s"x before: $x")
    x = 0
  }
  override def afterEach = {
    println(s"x after: $x")
  }

  val tests = Tests{
    'test1{
      x += 1
      assert(x == 1)
    }
    'test2{
      'inner{
        assert(x == 0)
      }
    }
    'test3{
      x += 42
      assert(x == 42)
    }
  }
}
