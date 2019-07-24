package test.utest
import utest._
import concurrent.Future
import utest.framework.ExecutionContext.RunNow

object FutureCrashTest extends TestSuite {
  def wrapping[T](f: => T):T = {
    f
  }

  def tests = Tests {
    "Crash the compiler when I spew a future" - {
      wrapping { val fut = Future { 1 } }
      // println("This prevents the crash")
    }
  }
}