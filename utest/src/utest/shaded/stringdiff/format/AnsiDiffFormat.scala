package utest.shaded.stringdiff.format

import utest.shaded.stringdiff.DiffElement

import scala.Console._

object AnsiDiffFormat extends DiffFormat[String] {

  import DiffElement._

  def apply(diff: List[DiffElement[String]]): String = {
    val sb = new StringBuilder
    sb.append(RESET)

    diff.foreach {
      case InBoth(both) =>
        sb.append("[")
        sb.append(UNDERLINED)
        sb.appendAll(both)
        sb.append(RESET)
        sb.append("]")
      case InSecond(second) =>
        sb.append("[∅|")
        sb.append(YELLOW)
        sb.append(UNDERLINED)
        sb.appendAll(second)
        sb.append(RESET)
        sb.append("]")
      case InFirst(first) =>
        sb.append("[")
        sb.append(RED)
        sb.append(UNDERLINED)
        sb.appendAll(first)
        sb.append(RESET)
        sb.append("|∅]")
      case Diff(first, second) =>
        sb.append("[")
        sb.append(RED)
        sb.append(UNDERLINED)
        sb.appendAll(first)
        sb.append(RESET)
        sb.append("|")
        sb.append(YELLOW)
        sb.append(UNDERLINED)
        sb.appendAll(second)
        sb.append(RESET)
        sb.append("]")
    }

    sb.toString()
  }

}
