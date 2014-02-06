package utest
package asserts

import scala.concurrent.duration._
import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.util.{Failure, Success, Try}

case class RetryInterval(d: FiniteDuration)
case class RetryMax(d: FiniteDuration)

object Concurrent {

  def eventuallyProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    TraceLogger(c)(q"utest.asserts.Concurrent.eventuallyImpl", exprs:_*)
  }
  def eventuallyImpl(funcs: (String, (LoggedValue => Unit) => Boolean)*)
                    (implicit interval: RetryInterval, max: RetryMax): Unit = {
    val start = Deadline.now
    @tailrec def rec(): Unit = {
      val result = for ((src, func) <- funcs) yield {
        val logged = collection.mutable.Buffer.empty[LoggedValue]
        (Try(func(logged.append(_))), logged, src)
      }
      val die = result.collectFirst{ case (Failure(_) | Success(false), logged, src) => (logged, src) }
      die match{
        case None =>
        case Some((logged, src)) =>
          if(Deadline.now < start + max.d){
            Thread.sleep(interval.d.toMillis)
            rec()
          }else{
            TraceLogger.throwError(
              "eventually " + src,
              result.filter(_._1 != Success(true)).flatMap(_._2)
            )
          }
      }
    }
    rec()
  }

  def continuallyProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    TraceLogger(c)(q"utest.asserts.Concurrent.continuallyImpl", exprs:_*)
  }

  def continuallyImpl(funcs: (String, (LoggedValue => Unit) => Boolean)*)
                     (implicit interval: RetryInterval, max: RetryMax): Unit = {
    val start = Deadline.now
    @tailrec def rec(): Unit = {
      val result = for ((src, func) <- funcs) yield {
        val logged = collection.mutable.Buffer.empty[LoggedValue]
        (Try(func(logged.append(_))), logged, src)
      }
      val die = result.collectFirst{ case (Failure(_) | Success(false), logged, src) => (logged, src) }
      die match{
        case Some((logged, src)) =>
          TraceLogger.throwError(
            "continually " + src,
            logged
          )
        case None if Deadline.now < start + max.d =>
          Thread.sleep(interval.d.toMillis)
          rec()
        case _ => ()

      }
    }
    rec()
  }
}

