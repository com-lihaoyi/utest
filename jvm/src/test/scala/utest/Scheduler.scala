package utest

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object Scheduler {
  lazy val system = akka.actor.ActorSystem()
  def scheduleOnce[T](interval: FiniteDuration)
                     (thunk: => T)
                     (implicit executor: ExecutionContext): Unit = {
    system.scheduler.scheduleOnce(interval)(thunk)
  }
}