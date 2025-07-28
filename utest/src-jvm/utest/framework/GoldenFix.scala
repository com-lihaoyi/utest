package utest.framework

case class GoldenFix(path: java.nio.file.Path,
                     contents: String,
                     startOffset: Int,
                     endOffset: Int)

object GoldenFix {
  @annotation.compileTimeOnly("implicit GoldenFix.Reporter instance is needed to call this method")
  implicit def reporter: Reporter = ???

  trait Reporter {
    def apply(v: GoldenFix): Unit
  }

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