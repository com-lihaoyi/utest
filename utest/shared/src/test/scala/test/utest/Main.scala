package test.utest
import utest._
/**
  * Created by lihaoyi on 10/9/17.
  */
object Main {
  def main(args: Array[String]): Unit = {

    val boa = new java.io.ByteArrayOutputStream()
    val tests = TestSuite{
      'test1-{
        throw new Exception("test1")
      }
      'test2-1

      'test3-{
        val a = List[Byte](1, 2)
        a(10)
      }
    }
    val results = utest.runWith(
      tests,
      utest.framework.Formatter,
      "MyTestSuite"
    )
    if (!results) System.exit(0)
  }
}
