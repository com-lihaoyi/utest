package utest.framework

object Executor extends Executor
trait Executor extends Formatter{
  def utestWrap(path: Seq[String], runBody: => concurrent.Future[Any])
               (implicit ec: concurrent.ExecutionContext): concurrent.Future[Any] = {
    runBody
  }

}
