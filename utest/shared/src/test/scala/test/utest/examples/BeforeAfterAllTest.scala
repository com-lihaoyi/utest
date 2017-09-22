package test.utest.examples

import utest._
import scala.concurrent.Future

object BeforeAfterAllSimpleTests extends TestSuite {

  override def utestBeforeAll(): Unit = {
    println("on before all")
  }
  override def utestAfterAll() = {
    println("on after all")
  }

  val tests = Tests {
    'outer1 - {
      'inner1 - {
        1
      }
      'inner2 - {
        2
      }
    }
  }
}

object BeforeAfterAllTests extends TestSuite {
  var x = 0
  override def utestBeforeAll(): Unit = {
    println(s"on before all x: $x")
    x = 100
  }
  override def utestAfterAll() = {
    println(s"on after all x: $x")
    assert(x == 116)
  }

  val tests = Tests {
    'outer1 - {
      x += 1
      'inner1 - {
        x += 2
        assert(x == 103) // += 100, += 1, += 2
        x
      }
      'inner2 - {
        Future.successful {
          x += 3
          assert(x == 107) // += 103, += 1, += 3
          x
        }
      }
    }
    'outer2 - {
      x += 4
      'inner3 - {
        x += 5
        assert(x == 116) // += 107, += 4, += 5
        x
      }
    }
  }
}
