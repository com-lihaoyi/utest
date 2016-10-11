package utest


import scala.annotation.meta._
import scala.util.{Failure, Success}
import scala.collection.mutable.ArrayBuffer

/**
 * Macro powered `assert`s of all shapes and sizes. These asserts all use
 * macros to capture the names, types and values of variables used within
 * them, so you get nice error messages for free.
 */
package object asserts extends utest.asserts.Asserts[DummyTypeclass]{
  val utestTruncateLength = 5000
  def assertPrettyPrint[T: DummyTypeclass](t: T) = t.toString.take(utestTruncateLength)

  type AssertEntry[T] = (String, (TestValue => Unit) => T)

  /**
   * Shorthand to quickly throw a utest.AssertionError, together with all the
   * macro-debugging goodness
   */
  def assertError(msgPrefix: String, logged: Seq[TestValue], cause: Throwable = null) = {
    throw AssertionError(
      msgPrefix + Option(cause).fold("")(e => s"\ncaused by: $e") + logged.map{
        case TestValue(name, tpe, thing) => s"\n$name: $tpe = $thing"
      }.mkString,
      logged,
      cause
    )
  }
  /**
    * Executes this AssertEntry and returns a successful result or dies in
    * case of failure. Even on success, it returns a die() function you can
    * call to manually throw and exception later if the result displeases you.
    */
  def getAssertionEntry[T](t: AssertEntry[T]): (T, Throwable => Nothing) = {
    val (res, logged, src) = runAssertionEntry(t)
    res match{
      case Success(value) => (value, t => assertError(src, logged, t))
      case Failure(e) => assertError(src, logged, e)
    }
  }

  /**
    * Executes this AssertEntry and returns the raw results
    */
  def runAssertionEntry[T](t: AssertEntry[T]) = {
    val (src, func) = t
    val logged = ArrayBuffer.empty[TestValue]
    val res = try {
      Success(func(logged.append(_)))
    } catch {
      case t: Throwable => Failure(t)
    }
    (res, logged, src)
  }

}
