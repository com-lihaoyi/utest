package utest.shaded.stringdiff
package util

import format.DiffFormat

private[stringdiff] object DiffLog {

  def log(description: => String, diff: => List[DiffElement[IndexedSeq[String]]]): Unit = {
    println(s"${description}:${" " * (20 - description.length)}${DiffFormat.ansi(diff.map(_.map(_.mkString)))}")
  }

  def logS(description: => String, diff: => List[DiffElement[String]]): Unit = {
    println(s"${description}:${" " * (20 - description.length)}${DiffFormat.ansi(diff)}")
  }

}
