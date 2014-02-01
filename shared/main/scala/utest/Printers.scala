package utest

import utest.framework.Result
import utest.util.Tree
import scala.util.{Failure, Success}
import java.io.{PrintWriter, StringWriter}

abstract class Formatter{
  /**
   * Prettyprints a single result.
   */
  def formatResult(path: Seq[String], r: Result): String

  /**
   * Prettyprints a tree of results; may or may not use `formatResult`.
   */
  def format(results: Tree[Result]): String
}

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

  def formatResult(path: Seq[String], r: Result): String = {
    val str = path.mkString(".") + "\t\t" + prettyTruncate(r)
    if (!trace) str
    else{
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      r.value match{
        case Failure(e) =>
//          pw.write("\n")
//          try{
//            e.printStackTrace(pw)
//          }catch{case e =>}
        case _ =>
      }
      str + sw.toString
    }
  }

  def format(results: Tree[Result]): String = {
    results.map(r => r.name + "\t\t" + prettyTruncate(r))
           .reduce(_ + _.map("\n" + _).mkString.replace("\n", "\n    "))
  }
}
