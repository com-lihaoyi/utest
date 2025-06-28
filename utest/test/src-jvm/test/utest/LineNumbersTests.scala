package test.utest

import utest._
import utest.framework.{TestCallTree, Tree}

object LineNumbersTests extends utest.TestSuite {

  val testsToRun = Tests {
    test("test1") {
      val x = 1
      assert(x == 2)
    }
    test("test2") {
      assert(1 == 2)
    }
    test("test3") {
      test("innerTest3")(assert(1 == 2))
    }
    test("test4") {
      test("innerTest4") {
        assert(1 == 2)
      }
    }

    test("test5") {
      test("innerTest5") {
        println("extra statement")
        assert(1 == 2)
      }
    }
  }

  val testBody = {

    val results = TestRunner.run(
      testsToRun
    )

    val stackTraceLinesFromThisFile = results.mapLeaves(_.value.failed.get.getStackTrace.toList.filter(_.getFileName == "LineNumbersTests.scala").toList).leaves.toList

    assert(
      stackTraceLinesFromThisFile(0).exists(_.getLineNumber == 11),
      stackTraceLinesFromThisFile(1).exists(_.getLineNumber == 14),
      stackTraceLinesFromThisFile(2).exists(_.getLineNumber == 17),
      stackTraceLinesFromThisFile(3).exists(_.getLineNumber == 21),
      stackTraceLinesFromThisFile(4).exists(_.getLineNumber == 28),

    )

  }

  val tests = Tests(Tree(""), new TestCallTree(Left(testBody))) //don't use Tests macro for stability

}



