package utest

/**
 * Created by haoyi on 2/5/14.
 */
object Main {
  def main(args: Array[String]): Unit = {
    val x = 1L
    val y = 0L
    assert(x / y == 10)
  }
}
import utest.ExecutionContext.RunNow
import utest.framework.{Result, TestSuite}
import scala.concurrent.ExecutionContext
import scala.util.Success




object MyTestSuite extends TestSuite{
  val tests = TestSuite{
    "hello" - {
      "world" - {
        val x = 1
        val y = 2
        assert(x != y)
        (x, y)
      }
    }
    "test2" - {
      val a = 1
      val b = 2
      assert(a == b)
    }
  }
}