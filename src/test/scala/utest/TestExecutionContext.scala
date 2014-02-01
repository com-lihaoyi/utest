package utest

import scala.concurrent.ExecutionContext

object TestExecutionContext{
  implicit val value = ExecutionContext.Implicits.global
}
