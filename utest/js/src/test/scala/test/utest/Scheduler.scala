package test.utest

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object Scheduler {
  def scheduleOnce[T](interval: FiniteDuration)
                     (thunk: => T)
                     (implicit executor: ExecutionContext): Unit = {
    scalajs.js.Dynamic.global.setTimeout(() => thunk, interval.toMillis)
  }
}
