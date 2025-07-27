package test.utest

import utest.framework.Tree
import utest._

abstract class MergeSubTests1 extends TestSuite {
  var run = Vector.empty[Int]
  override def tests = Tests {
    test("one") { run :+= 1 }
    test("two") { run :+= 2 }
    test("three") { run :+= 3 }
  }
}

abstract class MergeSubTests2 extends TestSuite {
  var run = Vector.empty[Int]
  override def tests = Tests {
    test("one") { run :+= 1 }
    test("two") { run :+= 2 }
  }
}

object MergeTestsTest extends TestSuite {

  val x = new MergeSubTests1 {}
  val y = new MergeSubTests2 {}

  val local = Tests {
    test("makeSureTestsRan") {
      test("x") { assert(x.run == Vector(1, 2, 3) ) }
      test("y") { assert(y.run == Vector(1, 2) ) }
    }
  }

  override def tests =
    x.tests.prefix("fst") ++
    y.tests.prefix("snd") ++
    local

  override def utestAfterAll(): Unit = {
    assert(x.run == Vector(1, 2, 3))
    assert(y.run == Vector(1, 2))
  }
}
