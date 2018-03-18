package test.utest.examples

import utest._
object HelloTests extends TestSuite{
  val tests = Tests{
    'test1{
//      throw new Exception("test1")
    }
    'test2{
      'inner{
        1
      }
    }
    'test3{
      val a = List[Byte](1, 2)
//      a(10)
    }
    'test4{
      val a = null
      a
    }
  }
}