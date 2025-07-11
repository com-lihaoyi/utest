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
    test("test6") {
      val result = "testing".trim()
      assert(result == "notMatching")
      result
    }
    test("test7") {
      val result = "testing".trim()
      assert(result == "notMatching")
      test("innerTest55") {
        assert(1 == 1)
      }
      result
    }
    test("test8") {
      val result = "testing".trim()
      val blockWithFailingAssert = {
        assert("matching" == "notMatching")
        assert(1 == 2)
      }
      test("innerTest55") {
        assert(1 == 1)
      }
      result
    }
    test("test9") {
      val result = "testing".trim()
      val methodWithFailingAssert = println(
        assert(1 == 2)
      )
      test("innerTest55") {
        assert(1 == 1)
      }
      result
    }
    test("test10") - Obj(1).method{ x =>
        assert(x.elem == "notMatching")
    }
    test("test11"){
        def xx(body: Int)(rest:Int) = body == rest
        val partiallyApplied = xx{
          assert(1 == 2)
          1
        } _

        ()
    }
    test("test12"){
        def xx(body: => Int)(rest:Int) = body == rest
        val partiallyAppliedByName = xx{
          assert(1 == 2)
          1
        } _

        partiallyAppliedByName(2)
        ()
    }
    test("test13") {
      object module
      val obj2 = new Object()
      Obj(1).method { eval =>


        var header = "header"
        assert(
          1 == 1,
          header == "//GPL"
        )

        println("")

        assert(
          1 == 2,
          header == "//MIT "
        )
      }
    }
    test("test14") {
      val result = "testing".trim()
      assert(
        result == "notMatching",
        1 == 2)
      result
    }
  }

  private case class Obj(arg:Int) {
    def method[T](tester: Obj => T):T = tester(this)
    val elem = "elem"
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
      stackTraceLinesFromThisFile(5).exists(_.getLineNumber == 33),
      stackTraceLinesFromThisFile(6).exists(_.getLineNumber == 38),
      stackTraceLinesFromThisFile(7).exists(_.getLineNumber == 47),
      stackTraceLinesFromThisFile(8).exists(_.getLineNumber == 58),
      stackTraceLinesFromThisFile(9).exists(_.getLineNumber == 66),
      stackTraceLinesFromThisFile(10).exists(_.getLineNumber == 71),
      stackTraceLinesFromThisFile(11).exists(_.getLineNumber == 80),
      stackTraceLinesFromThisFile(12).exists(_.getLineNumber == 94),
      stackTraceLinesFromThisFile(13).exists(_.getLineNumber == 109),
    )

  }

  val tests = Tests(Tree(""), new TestCallTree(Left(testBody))) //don't use Tests macro for stability

}



