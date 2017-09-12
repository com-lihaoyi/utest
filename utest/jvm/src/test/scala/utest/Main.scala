package utest

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by lihaoyi on 10/9/17.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val tests = Tests{
      'test1{
        //      throw new Exception("test1")
      }
      'test2{
        'inner{
          1
        }
      }
      'test3{
        val a = List[Byte](1, 2)
        //      a(10)
      }
    }

    TestRunner.run(tests)
    TestRunner.runAndPrint(
      tests,
      "MyTestSuite",
      executor = utest.framework.Executor,
      formatter = utest.framework.Formatter
    )
    TestRunner.runAndPrint(
      tests,
      "MyTestSuite",
      executor = new utest.framework.Executor{
        override def utestWrap(path: Seq[String], runBody: => Future[Any])
                     (implicit ec: ExecutionContext): Future[Any] = {
          runBody
        }
      },
      formatter = new utest.framework.Formatter{
        override def formatColor = false
      }
    )
  }
}
