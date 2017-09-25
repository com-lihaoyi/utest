package test.utest

import utest._
import scala.concurrent.Future
import concurrent.duration._


object BeforeAfterEachFailureTests extends TestSuite {
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
