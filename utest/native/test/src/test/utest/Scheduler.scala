package test.utest

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext

object Scheduler {
  // Will execute immediately, because we cannot currenly schedule
  // computation in Scala Native. (Execution happens either immediately
  // or at the end of the program.)
  def scheduleOnce[T](interval: FiniteDuration)
                     (thunk: => T)
                     (implicit executor: ExecutionContext): Unit = {
    executor.execute(new Runnable {
      def run(): Unit = thunk
    })
  }
}
