package utest

import scala.concurrent.Future

object FrameworkAsyncTests extends TestSuite{
  implicit val ec = utest.ExecutionContext.RunNow

  def tests = TestSuite{

    'asyncFailures {
      val tests = TestSuite {
        "testSuccess" - {
          Future {
            assert(true)
          }
        }
        "testFail" - {
          Future {
            assert(false)
          }
        }
      }

      tests.runAsync().map {
        results =>
          assert(results.toSeq.head.value.isSuccess)
          assert(results.toSeq.last.value.isFailure)
      }
    }
  }
}
