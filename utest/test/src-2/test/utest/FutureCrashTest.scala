package test.utest
import utest._
import concurrent.Future

object FutureCrashTest extends TestSuite {
  def wrapping[T](f: => T):T = {
    f
  }
  implicit val queue = utest.framework.ExecutionContext.RunNow

  def tests = TestSuite {
    "Crash the compiler when I spew a future" - {
      wrapping { val fut = Future { 1 } }
      // println("This prevents the crash")
    }
  }
}