package test.utest
import utest._
import concurrent.{Future, ExecutionContext}

object FutureTest extends TestSuite {
  implicit val ec: ExecutionContext = ExecutionContext.global
  @volatile var flag = false

  def tests = TestSuite {
    test("runs before next test"){
      Future { flag = true }
    }
    test("previous test ran first"){
      assert(flag)
    }
  }
}
