package utest

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Platform specific stuff that differs between JVM and JS
 */
object PlatformShims {
  def flatten[T](f: Future[Future[T]]): Future[T] = {
    f.value.get.map(_.value.get) match{
      case Success(Success(v)) => Future.successful(v)
      case Success(Failure(e)) => Future.failed(e)
      case Failure(e)          => Future.failed(e)
    }
  }

  def await[T](f: Future[T]): T = f.value.get.get
  def getTrace(e: Throwable): String = {
    ""
  }
}
