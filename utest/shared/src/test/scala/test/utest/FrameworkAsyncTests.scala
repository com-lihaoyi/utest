package test.utest

import utest._
import scala.concurrent.Future
import concurrent.duration._

object FrameworkAsyncTests extends TestSuite {
  implicit val ec = utest.framework.ExecutionContext.RunNow
  private val isNative = sys.props("java.vm.name") == "Scala Native"

  def tests = Tests {
    'hello {
      Future(10)
    }

    'asyncFailures {
      val tests = Tests {
        "testSuccessAsync" - {
          val p = concurrent.Promise[Int]
          test.utest.Scheduler.scheduleOnce(2 seconds)(p.success(123))

          // Not supported by Scala Native at the moment.
          // Futures are completed either at the end of the `main` function or immediately.
          // uTest will either consider that the test never completed, or too soon.
          assert(isNative || !p.isCompleted)

          p.future
        }
        "testFailAsync" - {
          val p = concurrent.Promise[Int]
          test.utest.Scheduler.scheduleOnce(2 seconds)(p.failure(new Exception("Boom")))

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

      TestRunner.runAsync(tests).map { results =>
        val leafResults = results.leaves.toSeq
        assert(leafResults(0).value.isSuccess) // testSuccessAsync
        assert(leafResults(1).value.isFailure) // testFailAsync
        assert(leafResults(2).value.isSuccess) // testSuccess
        assert(leafResults(3).value.isFailure) // testFail
        assert(leafResults(4).value.isSuccess) // normalSuccess
        assert(leafResults(5).value.isFailure) // normalFail
        assert(leafResults(6).value.isFailure) // testFailUnexpected
        assert(leafResults(7).value.isFailure) // testCastError
        leafResults(1).value
      }

    }
  }
}

object FrameworkBeforeAfterEachFailureTests extends TestSuite {
  implicit val ec = utest.framework.ExecutionContext.RunNow
  private var failNextBeforeEach = false
  private var failAfterEach = false

  override def utestBeforeEach(path: Seq[String]): Unit =
    if (failNextBeforeEach) {
      println(s"boom on before each running $path")
      failNextBeforeEach = false
      throw new Exception("boom on before each")
    } else ()
  override def utestAfterEach(path: Seq[String]): Unit =
    if (failAfterEach) {
      println(s"boom on after each running $path")
      failAfterEach = false
      throw new Exception("boom on after each")
    } else ()

  def tests = Tests {
    'hello {
      Future(10)
    }

    'beforeAfterEachFailures {
      def tests = Tests {
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
        'succeed - {
          failNextBeforeEach = true
          1
        }
        'failBecauseBeforeEachFailed - {
          ???
        }
        'passedButAfterEachFailed - {
          failAfterEach = true
          3
        }
      }

      TestRunner.runAsync(tests, executor = this).map { results =>
        val leafResults = results.leaves.toSeq
        assert(leafResults(0).value.isSuccess) // testSuccess
        assert(leafResults(1).value.isFailure) // testFail
        assert(leafResults(2).value.isSuccess) // normalSuccess
        assert(leafResults(3).value.isFailure) // normalFail
        assert(leafResults(4).value.isFailure) // testFailUnexpected
        assert(leafResults(5).value.isSuccess) // succeed
        assert(leafResults(6).value.isFailure) // failBecauseBeforeEachFailed
        assert(leafResults(7).value.isFailure) // passedButAfterEachFailed

        println(leafResults.count(_.value.isSuccess))
        assert(leafResults.count(_.value.isSuccess) == 3)
        assert(leafResults.count(_.value.isFailure) == 5)
        leafResults
      }
    }
  }
}
