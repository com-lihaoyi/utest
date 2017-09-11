package utest.asserts

import utest.{AssertionError, TestValue}

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

case class AssertEntry[T](label: String, thunk: (TestValue => Unit) => T)
/**
  * Created by lihaoyi on 9/9/17.
  */
object Util {

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
    val AssertEntry(src, func) = t
    val logged = ArrayBuffer.empty[TestValue]
    val res = Try(func(logged.append(_)))
    (res, logged, src)
  }

}
