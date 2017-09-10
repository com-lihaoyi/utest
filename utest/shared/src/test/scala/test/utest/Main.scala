package test.utest
import utest._
/**
  * Created by lihaoyi on 10/9/17.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val boa = new java.io.ByteArrayOutputStream()
    val printStream = new java.io.PrintStream(boa)
    val tests = TestSuite {
      'test1 - {
        val x = 1
        try assert(x == 2)
        catch{case e: Throwable =>
          throw new Exception("wrapper", e)
        }
      }
      'test2 - 1

      'test3 - {
        val a = List[Byte](1, 2)
        a(10)
      }
    }
    val results = utest.runWith(
      tests,
      new utest.framework.Formatter{
        override def formatWrapWidth = 50
      },
      "MyTestSuite"
    )
//    val boa = new java.io.ByteArrayOutputStream()
//    val tests = TestSuite{
//      'test1-{
//        throw new Exception("test1")
//      }
//      'test2-1
//
//      'test3-{
//        val a = List[Byte](1, 2)
//        a(10)
//      }
//    }
//    val results = utest.runWith(
//      tests,
//      utest.framework.Formatter,
//      "MyTestSuite"
//    )
//    if (!results) System.exit(0)
  }
}
