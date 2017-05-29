package test.utest
import utest._
import scala.concurrent.Future
import concurrent.duration._
object FrameworkAsyncTests extends utest.TestSuite{
  implicit val ec = utest.framework.ExecutionContext.RunNow
  private val isNative = sys.props("java.vm.name") == "Scala Native"

  def tests = this{
    'hello{
      Future(10)
    }

    'asyncFailures {
      val tests = this {
        "testSuccessAsync" - {
          val p = concurrent.Promise[Int]
          utest.Scheduler.scheduleOnce(2 seconds)(p.success(123))

          // Not supported by Scala Native at the moment.
          // Futures are completed either at the end of the `main` function or immediately.
          // uTest will either consider that the test never completed, or too soon.
          assert(isNative || !p.isCompleted)

          p.future
        }
        "testFailAsync" - {
          val p = concurrent.Promise[Int]
          utest.Scheduler.scheduleOnce(2 seconds)(p.failure(new Exception("Boom")))

          // Not supported by Scala Native at the moment.
          // Futures are completed either at the end of the `main` function or immediately.
          // uTest will either consider that the test never completed, or too soon.
          assert(isNative || !p.isCompleted)

          p.future
        }
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
        "normalSuccess" - {
          assert(true)
        }
        "normalFail" - {
          assert(false)
        }
        "testFailUnexpected" - {
          Future {
            throw new Exception("Lols boom")
          }
        }
        "testCastError" - {
          // These are Fatal in Scala.JS. Ensure they're handled else they freeze SBT.
          Future {
            // This test is disabled until scala-native/scala-native#858 is not fixed.
            assert(!isNative)
            0.asInstanceOf[String]
          }
        }
      }

      tests.runAsync().map { results =>
        assert(results.toSeq(0).value.isSuccess) // root
        assert(results.toSeq(1).value.isSuccess) // testSuccessAsync
        assert(results.toSeq(2).value.isFailure) // testFailAsync
        assert(results.toSeq(3).value.isSuccess) // testSuccess
        assert(results.toSeq(4).value.isFailure) // testFail
        assert(results.toSeq(5).value.isSuccess) // normalSuccess
        assert(results.toSeq(6).value.isFailure) // normalFail
        assert(results.toSeq(7).value.isFailure) // testFailUnexpected
        assert(results.toSeq(8).value.isFailure) // testCastError
        results.toSeq(1).value
      }

    }
  }
}
