package utest.framework

import scala.concurrent.ExecutionContext

object Executor extends Executor
trait Executor extends Formatter{
  def utestWrap(path: Seq[String], runBody: => concurrent.Future[Any])
               (implicit ec: ExecutionContext): concurrent.Future[Any] = {
    runBody
  }

}
