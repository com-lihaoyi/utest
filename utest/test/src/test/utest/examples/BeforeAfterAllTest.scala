package test.utest.examples

import utest._
import scala.concurrent.Future

object BeforeAfterAllSimpleTests extends TestSuite {
  println("on object body, aka: before all")

  override def utestAfterAll(): Unit = {
    println("on after all")
  }

  val tests = Tests {
    test("outer1"){
      test("inner1"){
        1
      }
      test("inner2"){
        2
      }
    }
  }
}

object BeforeAfterAllTests extends TestSuite {
  var x = 100
  println(s"starting with x: $x")

  override def utestAfterAll(): Unit = {
    println(s"on after all x: $x")
    assert(x == 116)
  }

  val tests = Tests {
    test("outer1"){
      x += 1
      test("inner1"){
        x += 2
        assert(x == 103) // += 100, += 1, += 2
        x
      }
      test("inner2"){
        Future.successful {
          x += 3
          assert(x == 107) // += 103, += 1, += 3
          x
        }
      }
    }
    test("outer2"){
      x += 4
      test("inner3"){
        x += 5
        assert(x == 116) // += 107, += 4, += 5
        x
      }
    }
  }
}
