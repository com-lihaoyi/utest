package utest.framework

object Executor extends Executor
trait Executor extends Formatter {
  def beforeEach: Unit = ()
  def afterEach: Unit = ()

  def utestWrap(path: Seq[String], runBody: => concurrent.Future[Any])
               (implicit ec: concurrent.ExecutionContext): concurrent.Future[Any] = {
    beforeEach

    runBody.andThen {
      case _ => afterEach
    }
  }
}
