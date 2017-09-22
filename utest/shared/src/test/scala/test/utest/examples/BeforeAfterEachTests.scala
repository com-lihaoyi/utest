package test.utest.examples

import utest._
object BeforeAfterEachTest extends TestSuite {
  var x = 0
  override def utestBeforeEach(): Unit = {
    println(s"on before each x: $x")
    x = 0
  }
  override def utestAfterEach() =
    println(s"on after each x: $x")

  val tests = Tests{
    'outer1 - {
      x += 1
      'inner1 - {
        x += 2
        assert(x == 3) // += 1, += 2
        x
      }
      'inner2 - {
        x += 3
        assert(x == 4) // += 1, += 3
        x
      }
    }
    'outer2 - {
      x += 4
      'inner3 - {
        x += 5
        assert(x == 9) // += 4, += 5
        x
      }
    }
  }
}
