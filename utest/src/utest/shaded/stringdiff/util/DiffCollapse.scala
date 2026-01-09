package utest.shaded.stringdiff
package util

import DiffElement.Diff
import DiffElement.InBoth
import DiffElement.InFirst
import DiffElement.InSecond

private[stringdiff] object DiffCollapse {

  def apply[A](
    diff: List[DiffElement[IndexedSeq[A]]]
  ): List[DiffElement[IndexedSeq[A]]] =
    ListScan(diff) {
      case InBoth(both) :: tail if both.isEmpty =>
        Nil -> tail

      case InFirst(first) :: tail if first.isEmpty =>
        Nil -> tail

      case InSecond(second) :: tail if second.isEmpty =>
        Nil -> tail

      case Diff(first, second) :: tail if first.isEmpty && second.isEmpty =>
        Nil -> tail

      case Diff(first, second) :: tail if second.isEmpty =>
        (InFirst(first) :: Nil) -> tail

      case Diff(first, second) :: tail if first.isEmpty =>
        (InSecond(second) :: Nil) -> tail

      case InBoth(both1) :: InBoth(both2) :: tail =>
        Nil -> (InBoth(both1 ++ both2) :: tail)

      case InSecond(second) :: InFirst(first) :: tail =>
        Nil -> (Diff(first, second) :: tail)

      case InFirst(first) :: InSecond(second) :: tail =>
        Nil -> (Diff(first, second) :: tail)

      case InFirst(first1) :: InFirst(first2) :: tail =>
        Nil -> (InFirst(first1 ++ first2) :: tail)

      case InSecond(second1) :: InSecond(second2) :: tail =>
        Nil -> (InSecond(second1 ++ second2) :: tail)

      case InFirst(first1) :: Diff(first2, second) :: tail =>
        Nil -> (Diff(first1 ++ first2, second) :: tail)

      case InSecond(second1) :: Diff(first, second2) :: tail =>
        Nil -> (Diff(first, second1 ++ second2) :: tail)

      case Diff(first1, second) :: InFirst(first2) :: tail =>
        Nil -> (Diff(first1 ++ first2, second) :: tail)

      case Diff(first, second1) :: InSecond(second2) :: tail =>
        Nil -> (Diff(first, second1 ++ second2) :: tail)

      case head :: tail =>
        (head :: Nil) -> tail

      case Nil => Nil -> Nil
    }

}
