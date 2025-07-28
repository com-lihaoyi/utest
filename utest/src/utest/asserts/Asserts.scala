package utest
package asserts

import utest.framework.{StackMarker, GoldenFix, SourceSpan}

import scala.annotation.{StaticAnnotation, tailrec}
import scala.collection.mutable
import scala.util.{Failure, Random, Success, Try}
import scala.reflect.ClassTag

/**
 * Macro implementation that provides rich error
 * message for boolean expression assertion.
 */
object Asserts extends AssertsCompanionVersionSpecific {

  def assertImpl(funcs0: AssertEntry[Boolean]*) = {
    val funcs = funcs0.toArray
    val failures = mutable.Buffer.empty[Throwable]
    var i = 0
    // Avoid using `.map` here, because it makes the stack trace tall, which
    // gets annoying to a user who is trying to look at the call stack and
    // figure out what's going on
    while(i < funcs.length){
      val (res, logged, src) = Util.runAssertionEntry(funcs(i))

      def prefix = if (funcs.length == 1) "" else s"#${i+1}: "
      res match{
        case Success(value) =>
          if (!value) throw Util.makeAssertError(prefix + src, logged, null)
        case Failure(e) => throw Util.makeAssertError(prefix + src, logged, e)
      }

      i += 1
    }

  }

  /**
   * Asserts that the given block raises the expected exception. The exception
   * is returned if raised, and an `AssertionError` is raised if the expected
   * exception does not appear.
   */
  def assertThrowsImpl[T: ClassTag](entry: AssertEntry[Unit]): T = {
    val (res, logged, src) = Util.runAssertionEntry(entry)
    res match{
      case Failure(e: T) => e
      case Failure(e: Throwable) => Util.assertError(src, logged, e)
      case Success(v) => Util.assertError(src, logged, null)
    }
  }

  /**
   * Asserts that the given block raises the expected exception. The exception
   * is returned if raised, and an `AssertionError` is raised if the expected
   * exception does not appear.
   */
  def assertMatchImpl(entry: AssertEntry[Any])
                     (pf: PartialFunction[Any, Unit]): Unit = StackMarker.dropInside{
    val (res, logged, src) = Util.runAssertionEntry(entry)
    res match{
      case Success(value) =>
        if (!pf.isDefinedAt(value)) throw Util.makeAssertError(
          src,
          logged,
          // Get the match error that would have been thrown by evaluating
          // the partial function, and wrap it in an AssertionError that would
          // capture any `val`s involved
          try{pf(value);???}catch{case e: Throwable => e}
        )
      case Failure(e) => throw Util.makeAssertError(src, logged, e)
    }
  }
}


trait Asserts extends AssertsVersionSpecific {
    /**
    * Provides a nice syntax for asserting things are equal, that is pretty
    * enough to embed in documentation and examples
    */
  implicit class ArrowAssert(lhs: Any){
    def ==>[V](rhs: V) = {
      (lhs, rhs) match{
          // Hack to make Arrays compare sanely; at some point we may want some
          // custom, extensible, typesafe equality check but for now this will do
          case (lhs: Array[_], rhs: Array[_]) =>
            Predef.assert(lhs.toSeq == rhs.toSeq, s"==> assertion failed: ${lhs.toSeq} != ${rhs.toSeq}")
          case (lhs, rhs) =>
            Predef.assert(lhs == rhs, s"==> assertion failed: $lhs != $rhs")
        }
    }
  }

  @tailrec final def retry[T](n: Int)(body: => T): T = {
    try body
    catch{case e: Throwable =>
      if (n > 0) retry(n-1)(body)
      else throw e
    }
  }

  def assertGoldenFile(path: java.nio.file.Path, testValue: String): Unit = {
    val goldenFileContents = java.nio.file.Files.readString(path)
    if (goldenFileContents != testValue) {
      if (!sys.env.contains("UTEST_UPDATE_GOLDEN_TESTS")) {
        throw new AssertionError(
          "Value does not match golden file contents: " + path,
          Seq(
            TestValue.Equality(
              TestValue.Single("goldenFileContents", None, goldenFileContents),
              TestValue.Single("testValue", None, testValue),
            )
          )
        )

      }
      else GoldenFix.register.value.apply(GoldenFix(path, goldenFileContents, 0, goldenFileContents.length))
    }
  }

  def assertGoldenLiteral(testValue: Any, golden: SourceSpan[Any]): Unit = {
    val goldenValue = golden.value
    if (testValue != goldenValue) {
      if (!sys.env.contains("UTEST_UPDATE_GOLDEN")) {
        throw new AssertionError(
          "Value does not match golden literal contents",
          Seq(
            TestValue.Equality(
              TestValue.Single("testValue", None, testValue),
              TestValue.Single("goldenValue", None, goldenValue),
            )
          )
        )
      } else {
       GoldenFix.register.value.apply(
         GoldenFix(
           java.nio.file.Path.of(golden.sourceFile),
           shaded.pprint.PPrinter.BlackWhite.apply(golden.value).plainText,
           golden.startOffset,
           golden.endOffset
         )
       )
      }
    }
  }

}
