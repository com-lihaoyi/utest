package utest
package framework
//import acyclic.file
object ExecutionContext{

  /**
   * `ExecutionContext` which runs any queued operations immediately.
   *
   * Useful if you don't want to deal with all the multithreading stuff,
   * as you can be sure the result will be ready as soon as any "async"
   * calls return.
   */
  implicit object RunNow extends scala.concurrent.ExecutionContext {
    def execute(runnable: Runnable) =
      try {
        runnable.run()
      } catch {

        case t: Throwable => throw t
      }

    def reportFailure(t: Throwable) = {
      Console.err.println("Failure in RunNow async execution: " + t)
      Console.err.println(t.getStackTrace.mkString("\n"))
    }
  }
}
