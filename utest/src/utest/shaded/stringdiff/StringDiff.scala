package utest.shaded.stringdiff

import utest.shaded.stringdiff.format.DiffFormat
import utest.shaded.stringdiff.util.DiffCollapse

object StringDiff {

  def apply(
    s1: String,
    s2: String,
    collapse: Boolean = true
  ): String = ansi(s1, s2, collapse)

  def ansi(
    s1: String,
    s2: String,
    collapse: Boolean = true
  ): String =
    DiffFormat.ansi(diff(s1, s2, collapse))

  def ansiBoth(
    s1: String,
    s2: String,
    collapse: Boolean = true
  ): (String, String) =
    DiffFormat.ansiBoth(diff(s1, s2, collapse))

  def text(
    s1: String,
    s2: String,
    collapse: Boolean = true
  ): String =
    DiffFormat.text(diff(s1, s2, collapse))

  def diff(
    s1: String,
    s2: String,
    collapse: Boolean = true
  ): List[DiffElement[String]] = {
    val v1 = s1.toVector
    val v2 = s2.toVector
    val myersDiff = MyersDiff.diff(v1, v2)
    val diff      = MyersInterpret(myersDiff, v1, v2)
    val result = if (collapse) {
      DiffCollapse(diff)
    } else {
      diff
    }
    result.map(_.map(_.mkString))
  }

}
