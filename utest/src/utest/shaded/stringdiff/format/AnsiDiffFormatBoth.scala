package utest.shaded.stringdiff.format

import utest.shaded.stringdiff.DiffElement

import scala.Console._

object AnsiDiffFormatBoth extends DiffFormat[(String, String)] {

  import DiffElement._

  def apply(diff: List[DiffElement[String]]): (String, String) = {
    val sb1 = new StringBuilder
    sb1.append(RESET)

    diff.foreach {
      case InBoth(both) =>
        sb1.append(UNDERLINED)
        sb1.appendAll(both)
        sb1.append(RESET)
      case InFirst(first) =>
        sb1.append(RED)
        sb1.append(UNDERLINED)
        sb1.appendAll(first)
        sb1.append(RESET)
      case InSecond(second) =>
        sb1.append(YELLOW)
        sb1.append(UNDERLINED)
        sb1.appendAll(second)
        sb1.append(RESET)
      case Diff(first, _) =>
        sb1.append(RED)
        sb1.append(UNDERLINED)
        sb1.appendAll(first)
        sb1.append(RESET)
    }

    val sb2 = new StringBuilder
    sb2.append(RESET)

    diff.foreach {
      case InBoth(both) =>
        sb2.append(UNDERLINED)
        sb2.appendAll(both)
        sb2.append(RESET)
      case InFirst(first) =>
        sb2.append(YELLOW)
        sb2.append(UNDERLINED)
        sb2.appendAll(first)
        sb2.append(RESET)
      case InSecond(second) =>
        sb2.append(RED)
        sb2.append(UNDERLINED)
        sb2.appendAll(second)
        sb2.append(RESET)
      case Diff(_, second) =>
        sb2.append(YELLOW)
        sb2.append(UNDERLINED)
        sb2.appendAll(second)
        sb2.append(RESET)
    }

    (sb1.toString(), sb2.toString())
  }

}
