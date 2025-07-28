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

  def columnLookup(index: Int, lineNumberLookup: Array[Int]): Int = {
    val line = lineNumberLookup.indexWhere(_ > index) match {
      case -1 => lineNumberLookup.length - 1
      case n => math.max(0, n - 1)
    }
    index - lineNumberLookup(line)
  }

  @annotation.compileTimeOnly("implicit GoldenFix.Reporter instance is needed to call this method")
  implicit def reporter: Reporter = ???

  trait Reporter {
    def apply(v: GoldenFix): Unit
  }

  def applyAll(fixes: Seq[GoldenFix]): Unit = {

    println("GoldenFix.applyAll: " + fixes.length)
    for((path, group) <- fixes.groupBy(_.path)){
      println("Updating golden fixes to file " + path)

      val text = java.nio.file.Files.readString(path)

      java.nio.file.Files.writeString(path, applyToText(text, group))
    }
  }

  def applyToText(text0: String, fixes: Seq[GoldenFix]): String = {
    var text = text0
    val sorted = fixes.sortBy(_.startOffset)

    val lineNumberLookupTable = lineNumberLookup(text)
    var lengthOffset = 0
    for (fix <- sorted) {
      val col = columnLookup(fix.startOffset, lineNumberLookupTable)
      val indentedContents = fix.contents.linesWithSeparators.mkString(" " * col)
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