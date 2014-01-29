package microtest.asserts

import scala.concurrent.duration._
import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.util.{Success, Try}

class RetryInterval(d: Duration)
class RetryMax(d: Duration)
object Concurrent {

  val interval = 100.millis
  val max = 1.second

  def eventuallyProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    TraceLogger(c)(q"microtest.asserts.Concurrent.eventuallyImpl", exprs:_*)
  }
  def eventuallyImpl(funcs: ((LoggedValue => Unit) => Boolean)*): Unit = {
    val start = Deadline.now

    @tailrec def rec(): Unit = {
      val result = for (func <- funcs) yield {
        val logged = collection.mutable.Buffer.empty[LoggedValue]
        (Try(func(logged.append(_))), logged)
      }
      if (result.forall(_._1 == Success(true))) ()
      else if(Deadline.now < start + max) {
        Thread.sleep(interval.toMillis)
        rec()
      } else {
        TraceLogger.throwError(
          "Eventually failed: ",
          result.filter(_._1 != Success(true)).flatMap(_._2)
        )
      }
    }
    rec()
  }

  def continuallyProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    TraceLogger(c)(q"microtest.asserts.Concurrent.continuallyImpl", exprs:_*)
  }

  def continuallyImpl(funcs: ((LoggedValue => Unit) => Boolean)*): Unit = {
    val start = Deadline.now
    @tailrec def rec(): Unit = {
      val result = for (func <- funcs) yield {
        val logged = collection.mutable.Buffer.empty[LoggedValue]
        (Try(func(logged.append(_))), logged)
      }

      if (!result.forall(_._1 == Success(true))) {

        TraceLogger.throwError(
          "Continually failed: ",
          result.filter(_._1 != Success(true)).flatMap(_._2)
        )
      } else if(Deadline.now < start + max) {
        Thread.sleep(interval.toMillis)
        rec()
      } else ()
    }
    rec()
  }
}

