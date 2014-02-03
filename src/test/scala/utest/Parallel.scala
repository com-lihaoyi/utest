package utest

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.Deadline
import scala.util.Random
import utest.framework.{TestSuite, Test}


object Parallel extends TestSuite{
  case class Counter(thunk: () => Unit){
    var i = 0
    def apply(){
      i += 1
      if (i > 5) thunk()
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

      def timedTrial(implicit ec: ExecutionContext) = {
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
        val error = intercept[LoggedAssertionError]{
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
        var x = Seq(12)
        var y = 0
        val i = Counter{ () =>
          x = Nil
          y = 1
        }

        eventually(
        {
          i()
          x == Nil
        },
          y == 1
        )

        (x, y)
      }
    }

    "continually"-{
      "failure"-{
        import ExecutionContext.RunNow
        var x = Seq(12)
        val y = 1

        val i = Counter(() => x = Nil)
        val error = intercept[LoggedAssertionError]{
          continually(
            {
              i()
              x == Seq(12)
            },
            y == 1
          )
        }

        val expected = utest.LoggedValue("x", "Seq[Int]", List())

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
