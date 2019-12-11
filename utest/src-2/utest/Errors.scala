package utest

trait CompileErrorVersionSpecific { this: CompileError =>
  def checkPositionString(expectedStripped: String, expectedRaw: String): Unit = {
    val actualNormalized = "\n" + pos
    if (expectedRaw != "") Predef.assert(
      actualNormalized == expectedStripped,
      "Compile error positions do not match\n" +
      "Expected Position\n" +
      expectedStripped + "\n" +
      "Actual Position\n" +
      actualNormalized
    )
  }
}