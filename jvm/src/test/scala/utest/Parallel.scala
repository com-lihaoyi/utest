package utest

import scala.concurrent.Future
import scala.concurrent.duration.Deadline
import scala.util.Random
import utest.asserts.{RetryInterval, RetryMax}


object Parallel extends TestSuite{
  case class Counter(){
    var i = 0
    def apply() = {
      i += 1
      i
    }
  }

  val tests = TestSuite{

    "parallelizingSpeedsThingsUp"-{
      def calc(n: Int, seed: Int) = {
        for(j <- 0 until 300000000){
          var x = 0
          var i = 0
          while(i < n){
            i += 1
            x = (x + 12312412) % seed
          }
        }
      }

      val tests = TestSuite{
        "test1"-calc(1000000000, Random.nextInt())
        "test2"-calc(1000000000, Random.nextInt())
        "test3"-calc(1000000000, Random.nextInt())
        "test4"-calc(1000000000, Random.nextInt())
      }

      def timedTrial(implicit ec: concurrent.ExecutionContext) = {
        val start = Deadline.now
        tests.run()
        val end = Deadline.now
        end - start
      }

      val parallelTime = timedTrial(concurrent.ExecutionContext.Implicits.global)

      val serialTime = timedTrial(utest.ExecutionContext.RunNow)
      val speedup = serialTime * 1.0 / parallelTime
      // Most people running this should be on at least a dual-core machine
      assert(speedup > 1.5)
      "Speedup: " + speedup
    }

    "eventually"-{
      "failure"-{
        val x = Seq(12)
        val y = 1
        val error = intercept[AssertionError]{
          eventually(
            x == Nil,
            y == 1
          )
        }
        val expected = Seq(utest.LoggedValue("x", "Seq[Int]", Seq(12)))
        assert(error.captured == expected)
        error.captured
      }
      "success"-{
        import ExecutionContext.RunNow

        val i = Counter()

        eventually(
          i() > 5
        )

        i()
      }
      "adjustInterval"-{
        import concurrent.duration._
        implicit val retryInterval = RetryInterval(300.millis)

        import ExecutionContext.RunNow

        val i = Counter()

        intercept[AssertionError]{
          eventually{
            i() > 5
          }
        }
      }

      "adjustMax"-{
        import concurrent.duration._
        implicit val retryMax = RetryMax(300.millis)

        import ExecutionContext.RunNow

        val i = Counter()

        intercept[AssertionError]{
          eventually{
            i() > 5
          }
        }
      }
    }

    "continually"-{
      "failure"-{
        import ExecutionContext.RunNow

        val i = Counter()
        val error = intercept[AssertionError]{
          continually(
            i() < 4
          )
        }

        val expected = utest.LoggedValue("i", "utest.Parallel.Counter", Counter())

        assert(error.captured.contains(expected))
        expected
      }
      "success"-{
        import ExecutionContext.RunNow
        val x = Seq(12)
        val y = 1

        continually(
          x == Seq(12),
          y == 1
        )
      }
    }
  }
}
