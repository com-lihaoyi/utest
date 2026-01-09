package utest.shaded.stringdiff

import utest.shaded.stringdiff.util.DiffCollapse

object SeqDiff {

  def seq[A](
    s1: IndexedSeq[A],
    s2: IndexedSeq[A],
    collapse: Boolean = true
  ): List[DiffElement[IndexedSeq[A]]] =
    apply(s1, s2, collapse)

  def apply[A](
    s1: IndexedSeq[A],
    s2: IndexedSeq[A],
    collapse: Boolean = true
  ): List[DiffElement[IndexedSeq[A]]] = {
    val myersDiff = MyersDiff.diff(s1, s2)
    val diff      = MyersInterpret(myersDiff, s1, s2)
    if (collapse) {
      DiffCollapse(diff)
    } else {
      diff
    }
  }

}
