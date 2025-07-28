package utest.framework

case class SourceSpan[T](value: T, sourceFile: String, startOffset: Int, endOffset: Int)
object SourceSpan extends SourceSpanMacros