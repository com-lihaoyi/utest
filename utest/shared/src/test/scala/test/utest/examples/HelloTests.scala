package test.utest.examples

import utest._
object HelloTests extends TestSuite{
  val tests = this{
    'test1{
      throw new Exception("test1")
    }
    'test2{
      1
    }
    'test3{
      val a = List[Byte](1, 2)
      a(10)
    }
  }
}