package test.utest

import utest._
import utest.framework.ExecutionContext.RunNow

/**
 * Put executor.utestAfterEach(path) into finally block to make sure it will be executed regardless of the test failing.
 */
object AfterEachOnFailureTest extends TestSuite {

  private var res:SomeResource = _

  override def utestBeforeEach(path: Seq[String]): Unit = {
    res = new SomeResource //open resource
  }

  override def utestAfterEach(path: Seq[String]): Unit = {
    res.close()
  }

  val tests = Tests{
    test("testFails") {
      val innerTests = Tests{
        throw new java.lang.AssertionError("Fail")
      }
      TestRunner.runAsync(innerTests, executor = this).map { results =>
        val leafResults = results.leaves.toSeq
        assert(leafResults(0).value.isFailure)
        assert(res.isClosed)
        leafResults
      }
    }
  }
  
  private class SomeResource{
    var isClosed:Boolean = false
    def close(): Unit = isClosed = true
  }
}
