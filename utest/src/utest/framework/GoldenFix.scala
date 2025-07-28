package utest.framework

case class GoldenFix(path: java.nio.file.Path,
                     contents: String,
                     startOffset: Int,
                     endOffset: Int)

object GoldenFix {
  def register = new scala.util.DynamicVariable[GoldenFix => Unit](_ => ())
  def applyAll(fixes: Seq[GoldenFix]): Unit = {
    for((path, group) <- fixes.groupBy(_.path)){
      val sorted = group.sortBy(_.startOffset)
      var txt = java.nio.file.Files.readString(path)
      for(fix <- sorted){
        txt = txt.patch(fix.startOffset, fix.contents, fix.endOffset - fix.startOffset)
      }
      java.nio.file.Files.writeString(path, txt)
    }
  }
}