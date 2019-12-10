package utest

trait CompileErrorVersionDependent { this: CompileError =>
  def checkPositionString(expectedStripped: String, expectedRaw: String): Unit =
    val actualNormalized = pos

    if (expectedRaw != "")
      if actualNormalized != expectedStripped
        println(s"""Compile error positions do not match
         |Expected Position
         |${expectedStripped}
         |Actual Position
         |${actualNormalized}""".stripMargin)
        Predef.assert(false, "Compile error positions do not match")
}
