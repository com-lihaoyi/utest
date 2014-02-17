package utest
import utest.framework.Result
import utest.util.Tree
import scala.util.{Failure, Success}
import java.io.{PrintWriter, StringWriter}

/**
 * Represents something that can format a single test result or a [[Tree]] of 
 * them. 
 */
abstract class Formatter{
  /**
   * Prettyprints a single result.
   */
  def formatSingle(path: Seq[String], r: Result): String

  /**
   * Prettyprints a tree of results; may or may not use `formatResult`.
   */
  def format(results: Tree[Result]): String
}
object DefaultFormatter{
  def apply(implicit args: Array[String]) = {
    val color = utest.util.ArgParse.find("--color", _.toBoolean, true, true)
    val truncate = utest.util.ArgParse.find("--truncate", _.toInt, 50, 50)
    val trace = utest.util.ArgParse.find("--trace", _.toBoolean, false, true)

    new DefaultFormatter(color, truncate, trace
    )
  }
}
/**
 * Default implementation of [[Formatter]], also used by the default SBT test
 * framework. Allows some degree of customization of the formatted test results.
 */
class DefaultFormatter(color: Boolean = true, 
                       truncate: Int = 30,
                       trace: Boolean = false) extends Formatter{
  
  def prettyTruncate(r: Result, length: Int = truncate): String = {

    val colorStr  = if (r.value.isSuccess) Console.GREEN else Console.RED
    val cutUnit =
      if (r.value == Success(())) "Success"
      else r.value.toString

    val s2 = if (color) colorStr + cutUnit + Console.RESET else cutUnit


    if (s2.length > length) s2.take(length) + "..."
    else s2
  }

  def formatSingle(path: Seq[String], r: Result): String = {
    val str = path.mkString(".") + "\t\t" + prettyTruncate(r)
    if (!trace) str
    else{
      val traceStr = r.value match{
        case Failure(e) => PlatformShims.getTrace(e)
        case _ =>
      }
      str + traceStr
    }
  }

  def format(results: Tree[Result]): String = {
    results.map(r => r.name + "\t\t" + prettyTruncate(r))
           .reduce(_ + _.map("\n" + _).mkString.replace("\n", "\n    "))
  }
}
