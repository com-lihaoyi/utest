
package test.utest

import utest._

object TestDiscoveryTests extends utest.TestSuite{

  val testsTopLevelStrings = Tests {
    test {  1  }
    test { "a" }
    test { throw new Exception("foo") }
    test {  2  }
    test { "b" }
  }

  val testsAnon = Tests {
    test("utest bug v2") {
      test { // name removed
        1 ==> 1
        "one"
      }
      test { // name removed
        2 ==> 2
        "two"
      }
    }
  }

  val tests = Tests{
    test - assert(testsTopLevelStrings.nameTree.children.length == 5)
    test - assert(testsAnon.nameTree.children(0).children.length == 2)
  }
}
