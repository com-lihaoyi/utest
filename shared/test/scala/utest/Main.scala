package utest
import utest.ExecutionContext.RunNow
/**
 * Created by haoyi on 2/5/14.
 */
object Main {
  def main(args: Array[String]): Unit = {
    val test = TestSuite{

      'test3{
        val a = List[Byte](1, 2)
        a(10)
      }
    }

  }
}
