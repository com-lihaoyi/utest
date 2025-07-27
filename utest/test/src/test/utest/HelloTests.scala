package test.utest

import utest._
object HelloTests extends TestSuite {
  val tests = Tests {
    test {  1  }

    test { "a" }

    test { throw new Exception("foo") }

    test {  2  }

    test { "b" }


  }
  shaded.pprint.log(tests)
}



