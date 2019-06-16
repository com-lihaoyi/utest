package test.utest

import utest._
import utest.framework.StackMarker


object BeforeAfterAllFailureTest extends TestSuite {

  // Hide this fella inside the outer object, because we don't want uTest's own
  // test suite to discover him: we want to run him manually
  object AfterAllFailureTest extends TestSuite {

    override def utestAfterAll(): Unit = {
      throw new Exception("Failed After!")
    }

    val tests = Tests {
      test("test1"){
        "win"
      }
      test("test2"){
        "win"
      }
    }
  }

  // No tests for this fella because currently, error handling of test suite
  // initialization is paid done in the SBT logic, not in TestRunner
  object BeforeAllFailureTest extends TestSuite {
    throw new Exception("Failed Before!")
    val tests = Tests {
      test("test"){
        "win"
      }
    }
  }


  val tests = Tests {
    test("afterAll"){
      val res = TestRunner.run(
        AfterAllFailureTest.tests,
        executor = AfterAllFailureTest
      )
      val failures = res.leaves
        .map(_.value)
        .collect{case f: scala.util.Failure[_] => f.exception}
        .toList
      val successes = res.leaves.count(_.value.isSuccess)
      val filteredStackLengths = failures.map(x => StackMarker.filterCallStack(x.getStackTrace).length)
      assert(
        successes == 0,
        failures.map(_.getMessage) == Seq("Failed After!"),
        // Make sure we return stack traces that get truncated to reasonably
        // small values, without all the external uninteresting stack frames
        filteredStackLengths.forall(_ < 15)
      )


      filteredStackLengths
    }
  }
}

