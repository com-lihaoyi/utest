package test.utest

import utest._
import scala.util.{
  Try, Success, Failure
}

object misc extends TestSuite {


  val tests = Tests {
    test("utest bug v2") {
      test - { // name removed
        1 ==> 1
        "one"
      }
      test { // name removed
        2 ==> 2
        "two"
      }
    }
  }
  shaded.pprint.log(tests)
}
