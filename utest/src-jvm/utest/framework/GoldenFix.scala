package utest.framework

import scala.collection.mutable.ArrayBuffer

case class GoldenFix(path: java.nio.file.Path,
                     contents: String,
                     startOffset: Int,
                     endOffset: Int)

object GoldenFix {
  // vendored from
  // https://github.com/com-lihaoyi/fastparse/blob/d8f95daef21d6e6f9734624237f993f4cebfa881/fastparse/src/fastparse/internal/Util.scala
  //
  def lineNumberLookup(data: String): Array[Int] = {
    val lineStarts = ArrayBuffer[Int](0)
    var i = 0
    var col = 1
    // Stores the previous char we saw, or -1 if we just saw a \r\n or \n\r pair
    var state: Int = 0
    while (i < data.length) {
      val char = data(i)
      if (char == '\r' && state == '\n' || char == '\n' && state == '\r') {
        col += 1
        state = -1
      } else if (state == '\r' || state == '\n' || state == -1) {
        lineStarts.append(i)
        col = 1
        state = char
      } else {
        col += 1
        state = char
      }

      i += 1
    }

    if (state == '\r' || state == '\n' || state == -1) {
      lineStarts.append(i)
    }

    lineStarts.toArray
  }

  def lineColumnLookup(index: Int, lineNumberLookup: Array[Int]): (Int, Int) = {
    val line = lineNumberLookup.indexWhere(_ > index) match {
      case -1 => lineNumberLookup.length - 1
      case n => math.max(0, n - 1)
    }
    (line, index - lineNumberLookup(line))
  }

  trait Reporter {
    def apply(v: GoldenFix): Unit
  }

  def applyAll(fixes: Seq[GoldenFix]): Unit = {
    for((path, group) <- fixes.groupBy(_.path)){
      println(s"UTEST_UPDATE_GOLDEN_TESTS detected, uTest applying ${group.size} golden fixes to file $path")
      val text = java.nio.file.Files.readString(path)
      java.nio.file.Files.writeString(path, applyToText(text, group))
    }
  }

  def applyToText(text0: String, fixes: Seq[GoldenFix]): String = {
    var text = text0
    val sorted = fixes.map(t => t.startOffset -> t).toMap.map(_._2).toSeq.sortBy(_.startOffset)
    sorted.sliding(2).collect{ case Seq(prev, next) =>
      Predef.assert(prev.endOffset <= next.startOffset)
    }

    val lineNumberLookupTable = lineNumberLookup(text)
    var lengthOffset = 0
    for (fix <- sorted) {
      val (startLine, startCol) = lineColumnLookup(fix.startOffset, lineNumberLookupTable)
      val (endLine, endCol) = lineColumnLookup(fix.endOffset, lineNumberLookupTable)
      println(s"Updating line:column $startLine:$startCol to $endLine:$endCol")
      val indentedContents = fix.contents.linesWithSeparators.mkString(" " * startCol)
      text = text.patch(
        fix.startOffset + lengthOffset, indentedContents,
        fix.endOffset - fix.startOffset
      )
      lengthOffset = lengthOffset + indentedContents.length - (fix.endOffset - fix.startOffset)
    }

    text
  }

  case class Span[+T](value: T, sourceFile: String, startOffset: Int, endOffset: Int)

  object Span extends GoldenSpanMacros

}