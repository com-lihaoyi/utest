package utest
package asserts
//import acyclic.file
import scala.concurrent.duration._
import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.util.{Failure, Success, Try}

/**
 * Used to specify a retry-interval for the `eventually` and
 * `continually` asserts.
 */
case class RetryInterval(d: FiniteDuration)
/**
 * Used to specify a maximum retry duration for the `eventually`
 * and `continually` asserts.
 */
case class RetryMax(d: FiniteDuration)

/**
 * Asserts which only make sense when running on multiple threads.
 */
object Parallel {

  def eventuallyProxy(c: Context)(exprs: c.Expr[Boolean]*): c.Expr[Unit] = {
    import c.universe._
    Tracer[Boolean](c)(q"utest.asserts.Parallel.eventuallyImpl", exprs:_*)
  }
  def eventuallyImpl(funcs: AssertEntry[Boolean]*)
                    (implicit interval: RetryInterval, max: RetryMax): Unit = {
    val start = Deadline.now
    @tailrec def rec(): Unit = {
      val result = funcs.map(Util.runAssertionEntry)

      val die = result.collectFirst{ case (Failure(_) | Success(false), logged, src) => (logged, src) }
      die match{
        case None =>
        case Some((logged, src)) =>
          if(Deadline.now < start + max.d){
            Thread.sleep(interval.d.toMillis)
            rec()
          }else{
            Util.assertError(
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
    Tracer[Boolean](c)(q"utest.asserts.Parallel.continuallyImpl", exprs:_*)
  }

  def continuallyImpl(funcs: AssertEntry[Boolean]*)
                     (implicit interval: RetryInterval, max: RetryMax): Unit = {
    val start = Deadline.now
    @tailrec def rec(): Unit = {
      val result = funcs.map(Util.runAssertionEntry)
      val die = result.collectFirst{ case (Failure(_) | Success(false), logged, src) => (logged, src) }
      die match{
        case Some((logged, src)) =>
          Util.assertError(
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

