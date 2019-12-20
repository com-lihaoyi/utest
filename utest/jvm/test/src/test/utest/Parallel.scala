package test.utest
import utest._
import utest.asserts.{RetryInterval, RetryMax}


object Parallel extends TestSuite{
  case class Counter(){
    var i = 0
    def apply() = {
      i += 1
      i
    }
  }

  val tests = Tests{

//    "parallelizingSpeedsThingsUp"-{
//      def calc(n: Int, seed: Int) = {
//        for(j <- 0 until 300000000){
//          var x = 0
//          var i = 0
//          while(i < n){
//            i += 1
//            x = (x + 12312412) % seed
//          }
//        }
//      }
//
//      val tests = Tests{
//        "test1"-calc(1000000000, Random.nextInt())
//        "test2"-calc(1000000000, Random.nextInt())
//        "test3"-calc(1000000000, Random.nextInt())
//        "test4"-calc(1000000000, Random.nextInt())
//      }
//
//      def timedTrial(implicit ec: concurrent.ExecutionContext) = {
//        val start = Deadline.now
//        tests.run()
//        val end = Deadline.now
//        end - start
//      }
//
//      val parallelTime = timedTrial(concurrent.ExecutionContext.Implicits.global)
//
//      val serialTime = timedTrial(utest.ExecutionContext.RunNow)
//      val speedup = serialTime * 1.0 / parallelTime
//      // Most people running this should be on at least a dual-core machine
//      assert(speedup > 1.5)
//      "Speedup: " + speedup
//    }

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
        val expected = Seq(utest.TestValue("x", "Seq[Int]", Seq(12)))
        assert(error.captured == expected)
        error.captured
      }
      "success"-{

        val i = Counter()

        eventually(
          i() > 5
        )

        i()
      }
      "adjustInterval"-{
        import concurrent.duration._
        implicit val retryInterval: RetryInterval = RetryInterval(300.millis)

        val i = Counter()

        intercept[AssertionError]{
          eventually{
            i() > 5
          }
        }
      }

      "adjustMax"-{
        import concurrent.duration._
        implicit val retryMax: RetryMax = RetryMax(300.millis)

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

        val i = Counter()
        val error = intercept[AssertionError]{
          continually(
            i() < 4
          )
        }

        val expected = utest.TestValue("i", "test.utest.Parallel.Counter", Counter())

        assert(error.captured.contains(expected))
        expected
      }
      "success"-{
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
