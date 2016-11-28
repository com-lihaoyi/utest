package utest

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import concurrent.duration._
import java.io.{PrintWriter, StringWriter}

/**
 * Platform specific stuff that differs between JVM and JS
 */
object PlatformShims {
  def await[T](f: Future[T]): T = Await.result(f, 10.hours)
}
