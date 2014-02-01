package utest

import scala.concurrent.ExecutionContext.Implicits.global
import utest.framework.{Result, TestSuite}
import scala.concurrent.ExecutionContext
import scala.util.Success


object Main {
  def main(args: Array[String]): Unit = {
    val test = TestSuite{
      "test1"-{
        throw new Exception("test1")
      }
      "test2"-{
        1
      }
      "test3"-{
        val a = List[Byte](1, 2)
        a(10)
      }
    }
    val formatter = new DefaultFormatter()
    println(formatter.format(test.run()))
    // Result(Main$,Success(()),Deadline(1391217279134768000 nanoseconds),Deadline(1391217279158741000 nanoseconds))
    // Result(test1,Failure(java.lang.Exception: test1),Deadline(1391217279151624000 nanoseconds),Deadline(1391217279155890000 nanoseconds))
    // Result(test2,Success(1),Deadline(1391217279151801000 nanoseconds),Deadline(1391217279155629000 nanoseconds))
    // Result(test3,Failure(java.lang.IndexOutOfBoundsException: 10),Deadline(1391217279151704000 nanoseconds),Deadline(1391217279155983000 nanoseconds))
  }
}

