package utest.framework

object Executor extends Executor

trait Executor {
  def utestBeforeEach(path: Seq[String]): Unit = ()
  def utestAfterEach(path: Seq[String]): Unit = ()
  def utestAfterAll(): Unit = ()

  def utestWrap(path: Seq[String], runBody: => concurrent.Future[Any])
               (implicit ec: concurrent.ExecutionContext): concurrent.Future[Any] = {
    runBody
  }
}
