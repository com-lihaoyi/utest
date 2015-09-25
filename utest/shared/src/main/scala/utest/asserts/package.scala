package utest

import utest.framework.{AssertionError, LoggedValue}

import scala.util.{Failure, Success, Try}
import scala.collection.mutable.ArrayBuffer

/**
 * Macro powered `assert`s of all shapes and sizes. These asserts all use
 * macros to capture the names, types and values of variables used within
 * them, so you get nice error messages for free.
 */
package object asserts {
  type AssertEntry[T] = (String, (LoggedValue => Unit) => T)

  /**
   * Shorthand to quickly throw a utest.AssertionError, together with all the
   * macro-debugging goodness
   */
  def assertError(msgPrefix: String, logged: Seq[LoggedValue], cause: Throwable = null) = {
    throw AssertionError(
      msgPrefix + Option(cause).fold("")(e => s"\ncaused by: $e") + logged.map{
        case LoggedValue(name, tpe, thing) => s"\n$name: $tpe = $thing"
      }.mkString,
      logged,
      cause
    )
  }
  
  implicit class AssertEntryExt[T](t: AssertEntry[T]){
    val (src, func) = t
    /**
     * Executes this AssertEntry and returns a successful result or dies in
     * case of failure. Even on success, it returns a die() function you can
     * call to manually throw and exception later if the result displeases you.
     */
    def get(): (T, Throwable => Nothing) = {
      val (res, logged, src) = run()
      res match{
        case Success(value) => (value, t => assertError(src, logged, t))
        case Failure(e) => assertError(src, logged, e)
      }
    }

    /**
     * Executes this AssertEntry and returns the raw results
     */
    def run() = {
      val logged = ArrayBuffer.empty[LoggedValue]
      val res = Try(func(logged.append(_)))
      (res, logged, src)
    }
  }
}
