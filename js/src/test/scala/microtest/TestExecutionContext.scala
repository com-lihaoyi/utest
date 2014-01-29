package microtest

import scala.scalajs.concurrent.JSExecutionContext

object TestExecutionContext{
  implicit val value = JSExecutionContext.Implicits.queue
}