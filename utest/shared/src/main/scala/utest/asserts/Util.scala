package utest.asserts

import utest.framework.StackMarker
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
  def assertError(msgPrefix: String, logged: collection.Seq[TestValue], cause: Throwable = null) = {
    throw makeAssertError(msgPrefix, logged, cause)
  }

  def makeAssertError(msgPrefix: String,
                      logged: collection.Seq[TestValue],
                      cause: Throwable = null) = StackMarker.dropInside{
    val err = AssertionError(msgPrefix, logged.toSeq, cause)
    if (cause != null) err.setStackTrace(Array.empty)
    err
  }

  /**
    * Executes this AssertEntry and returns the raw results
    */
  def runAssertionEntry[T](t: AssertEntry[T]) = {
    val AssertEntry(src, func) = t
    val logged = ArrayBuffer.empty[TestValue]
    val res =
      try Success(func(logged.append(_)))
      catch{case e: Throwable => Failure(e)}

    (res, logged, src)
  }

}
