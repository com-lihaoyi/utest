package test.utest
import utest._
import scala.concurrent.{ExecutionContext, Future}

object Main {
  def main(args: Array[String]): Unit = {
    val tests = Tests{
      test("test1"){
        // throw new Exception("test1")
      }
      test("test2"){
        test("inner"){
          val a = 1
          val b = 2
          val c = 3
          val d = 4
          assert(
            a == 1,
            c == d,
            ???
          )
        }
      }
      test("test3"){
        val a = List[Byte](1, 2)
        // a(10)
      }
    }


    // Run, return results, and print streaming output with the default formatter
    val results2 = TestRunner.runAndPrint(
      tests,
      "MyTestSuiteA"
    )
  }
}
