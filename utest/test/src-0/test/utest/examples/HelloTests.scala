package test.utest.examples

import utest._
object HelloTests extends TestSuite{
  val tests = Tests{
    Symbol("test1"){
//      throw new Exception("test1")
    }
    Symbol("test2"){
      Symbol("inner"){
        1
      }
    }
    Symbol("test3"){
      val a = List[Byte](1, 2)
//      a(10)
    }
    Symbol("test4"){
      val a = null
      a
    }
  }
}