package utest

trait CompileErrorVersionSpecific { this: CompileError =>
  val normalizedPos = pos
}
