package utest.framework

import scala.concurrent.Future

object Executor extends Executor

trait Executor extends Formatter {
  def utestBeforeEach(): Unit = ()
  def utestAfterEach(): Unit = ()
  def utestBeforeEach(path: Seq[String]): Unit = utestBeforeEach()
  def utestAfterEach(path: Seq[String]): Unit = utestAfterEach()
  def utestAfterAll(): Unit = ()

  def utestWrap(path: Seq[String], runBody: => concurrent.Future[Any])
               (implicit ec: concurrent.ExecutionContext): concurrent.Future[Any] =
    for {
      _      <- Future { utestBeforeEach(path) }
      result <- runBody
      _      <- Future { utestAfterEach(path) }
    } yield result
}
