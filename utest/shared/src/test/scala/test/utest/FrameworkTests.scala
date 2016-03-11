package test.utest
import utest._


object FrameworkTests extends TestSuite{
  TestSuite{
    val x = 1
    TestableSymbol('outer) {
      val y = x + 1
    }
  }
}
