package test.utest
import utest._
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object Scheduler {
  private val scheduler = new ScheduledThreadPoolExecutor(1)
  def scheduleOnce[T](interval: FiniteDuration)
                     (thunk: => T)
                     (implicit executor: ExecutionContext): Unit = {
    scheduler.schedule(new Runnable {
      def run(): Unit = { thunk }
    }, interval.toMillis, TimeUnit.MILLISECONDS)
  }
}
