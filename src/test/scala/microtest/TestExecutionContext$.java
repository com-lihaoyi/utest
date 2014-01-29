package microtest;

object TestExecutionContext{
  implicit val value = ExecutionContext.Implicits.global
}
