package utest.shaded.stringdiff.format

import utest.shaded.stringdiff.DiffElement

object TextDiffFormat extends DiffFormat[String] {

  import DiffElement._

  def apply(diff: List[DiffElement[String]]): String = {
    val sb = new StringBuilder
    diff.foreach {
      case InBoth(both) =>
        sb.append("]")
        sb.appendAll(both)
        sb.append("]")
      case InSecond(second) =>
        sb.append("[∅|")
        sb.appendAll(second)
        sb.append("]")
      case InFirst(first) =>
        sb.append("[")
        sb.appendAll(first)
        sb.append("|∅]")
      case Diff(first, second) =>
        sb.append("[")
        sb.appendAll(first)
        sb.append("|")
        sb.appendAll(second)
        sb.append("]")
    }
    sb.toString()
  }

}
