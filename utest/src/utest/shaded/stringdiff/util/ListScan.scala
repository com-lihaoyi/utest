package utest.shaded.stringdiff.util

import scala.collection.mutable.ListBuffer

private[stringdiff] object ListScan {

  def apply[A, B](list: List[A])(f: List[A] => (List[B], List[A])): List[B] = {
    withBuffer[A, B](list) { (list, buffer) =>
      val (toBuffer, newWork) = f(list)
      buffer.appendAll(toBuffer)
      newWork
    }
  }

  def withBuffer[A, B](list: List[A])(f: (List[A], ListBuffer[B]) => List[A]): List[B] = {
    val buffer = new ListBuffer[B]
    var work   = list
    while (work.nonEmpty) {
      work = f(work, buffer)
    }
    buffer.toList
  }

}
