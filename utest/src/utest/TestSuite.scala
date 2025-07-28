package utest

import utest.framework.{Formatter, TestSuitePlatformSpecific}
import scala.concurrent.{ExecutionContext, Future}
import utest.framework.PlatformShims.EnableReflectiveInstantiation

/**
 * Marker class used to mark an `object` as something containing tests. Used
 * for test-discovery by SBT.
 */
@EnableReflectiveInstantiation
abstract class TestSuite
  extends framework.Executor with TestSuiteVersionSpecific with TestSuitePlatformSpecific {
  def utestFormatter: Formatter = null
  def tests: Tests
}

object TestSuite extends TestSuiteCompanionVersionSpecific {
  trait Retries extends utest.TestSuite{
    def utestRetryCount: Int
    override def utestWrap(path: Seq[String], body: => Future[Any])(implicit ec: ExecutionContext): Future[Any] = {
      def rec(count: Int): Future[Any] = {
        val result = for {
          _      <- Future { utestBeforeEach(path) }
          result <- body
          _      <- Future { utestAfterEach(path) }
        } yield result
        result.recoverWith { case e =>
          if (count < utestRetryCount) rec(count + 1)
          else Future.failed(e)
        }
      }
      val res = rec(0)
      res
    }
  }
}


