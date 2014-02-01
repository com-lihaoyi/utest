package utest

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import concurrent.duration._
/**
 * Created by haoyi on 1/31/14.
 */
object Flattener {
  def flatten[T](f: Future[Future[T]])(implicit ec: ExecutionContext): Future[T] = f.flatMap(x => x)

  def await[T](f: Future[T]): T = Await.result(f, 10.hours)
}
