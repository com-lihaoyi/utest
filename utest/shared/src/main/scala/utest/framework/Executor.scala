package utest.framework

object Executor extends Executor
trait Executor extends Formatter {
  def utestBeforeEach(): Unit = ()
  def utestAfterEach(): Unit = ()
  def utestBeforeAll(): Unit = ()
  def utestAfterAll(): Unit = ()

  def utestWrap(path: Seq[String], runBody: => concurrent.Future[Any])
               (implicit ec: concurrent.ExecutionContext): concurrent.Future[Any] = {
    utestBeforeEach()

    runBody.andThen {
      case _ => utestAfterEach()
    }
  }
}
