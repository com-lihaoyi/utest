package test.utest
import utest._
import utest.asserts.{RetryInterval, RetryMax}


object Parallel extends TestSuite{
  implicit val colors: shaded.pprint.TPrintColors = shaded.pprint.TPrintColors.Colors
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

    "assertEventually"-{
      "failure"-{
        val x = Seq(12)
        val y = 1
        val error = assertThrows[AssertionError]{
          assertEventually(x == Nil && y == 1)
        }

        val expected = Seq(
          utest.TestValue.Single("x", Some(shaded.pprint.tprint[Seq[Int]]), Seq(12)),
          utest.TestValue.Equality(
            utest.TestValue.Single("x", None, Seq(12)),
            utest.TestValue.Single("Nil", None, Seq())
          )
        )
        val expectedScala3 = Seq(
          utest.TestValue.Single("x", Some(shaded.pprint.tprint[Seq[Int]]), Seq(12)),
          utest.TestValue.Equality(
            utest.TestValue.Single("x", None, Seq(12)),
            utest.TestValue.Single("Nil", None, Seq())
          )
        )


        assert(error.captured == expected || error.captured == expectedScala3)
        error.captured
      }

      "success"-{
        val i = Counter()

        assertEventually(
          i() > 5
        )

        i()
      }
      "adjustInterval"-{
        import concurrent.duration._
        implicit val retryInterval: RetryInterval = RetryInterval(300.millis)

        val i = Counter()

        assertThrows[AssertionError]{
          assertEventually{
            i() > 5
          }
        }
      }

      "adjustMax"-{
        import concurrent.duration._
        implicit val retryMax: RetryMax = RetryMax(300.millis)

        val i = Counter()

        assertThrows[AssertionError]{
          assertEventually{
            i() > 5
          }
        }
      }
    }

    "assertContinually"-{
      "failure"-{

        val i = Counter()
        val error = assertThrows[AssertionError]{
          assertContinually(
            i() < 4
          )
        }

        val expected = utest.TestValue.Single("i", Some(shaded.pprint.tprint[Parallel.Counter]), Counter())

        assert(error.captured.contains(expected))
        expected
      }
      "success"-{
        val x = Seq(12)
        val y = 1

        assertContinually(x == Seq(12) && y == 1)
      }
    }
  }
}
