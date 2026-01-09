package utest.shaded.stringdiff

import utest.shaded.stringdiff.format.DiffFormat
import utest.shaded.stringdiff.util.DiffCollapse
import utest.shaded.stringdiff.util.DiffPrettier
import utest.shaded.stringdiff.util.DiffTokenize
import scala.collection.mutable.ListBuffer

object TokenDiff {

  private val whitespace = "\\s+".r
  private def tokenize(s: String): IndexedSeq[String] = {
    val buffer = new ListBuffer[String]
    var pos    = 0
    whitespace.findAllMatchIn(s).foreach { m =>
      if (m.start > pos) {
        buffer.append(s.substring(pos, m.start))
      }
      buffer.append(s.substring(m.start, m.end))
      pos = m.end
    }
    if (pos < s.length) {
      buffer.append(s.substring(pos))
    }
    buffer.toVector
  }

  def apply(
    s1: String,
    s2: String
  ): String = ansi(s1, s2)

  def ansi(
    s1: String,
    s2: String
  ): String =
    DiffFormat.ansi(diff(s1, s2))

  def ansiBoth(
    s1: String,
    s2: String
  ): (String, String) =
    DiffFormat.ansiBoth(diff(s1, s2))

  def text(
    s1: String,
    s2: String
  ): String =
    DiffFormat.ansi(diff(s1, s2))

  def diff[Out](
    s1: String,
    s2: String
  ): List[DiffElement[String]] = {
    val diff = SeqDiff(
      TokenDiff.tokenize(s1),
      TokenDiff.tokenize(s2),
      collapse = false
    )
    var in          = diff
    var transformed = diff
    var first       = true
    while (first || in != transformed) {
      first = false
      in = transformed
      transformed = DiffTokenize.moveWhitespace(transformed)
      transformed = DiffTokenize.firstsGoFirst(transformed)
    }
    transformed = DiffTokenize.join(transformed)
    transformed = DiffCollapse(transformed)
    DiffPrettier(transformed.map(_.map(_.mkString)))
  }

}
