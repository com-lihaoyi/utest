package utest.shaded.stringdiff

object MyersDiff {

  def diff[A](ss1: IndexedSeq[A], ss2: IndexedSeq[A]): Seq[Operation] = {

    def mod(a: Int, b: Int): Int = {
      (b + (a % b)) % b
    }

    def rec(
      ss1: IndexedSeq[A],
      ss2: IndexedSeq[A],
      i: Int,
      j: Int
    ): Seq[Operation] = {
      val N = ss1.size
      val M = ss2.size
      val L = N + M
      val Z = 2 * Math.min(N, M) + 2
      if (N > 0 && M > 0) {
        val w = N - M
        val g = Array.fill(Z)(0)
        val p = Array.fill(Z)(0)
        for (h <- 0 until (L / 2 + mod(L, 2)) + 1) {
          for (r <- 0 until 2) {
            val (c, d, o, m) = if (r == 0) (g, p, 1, 1) else (p, g, 0, -1)
            for (k <- -(h - 2 * Math.max(0, h - M)) until h - 2 * Math.max(0, h - N) + 1 by 2) {
              var a =
                if (k == -h || k != h && c(mod(k - 1, Z)) < c(mod(k + 1, Z))) {
                  c(mod(k + 1, Z))
                } else {
                  c(mod(k - 1, Z)) + 1
                }
              var b      = a - k
              val (s, t) = (a, b)
              while (a < N && b < M && ss1((1 - o) * N + m * a + (o - 1)) == ss2((1 - o) * M + m * b + (o - 1))) {
                a = a + 1
                b = b + 1
              }
              c(mod(k, Z)) = a
              val z = -(k - w)
              if (mod(L, 2) == o && z >= -(h - o) && z <= h - o && c(mod(k, Z)) + d(mod(z, Z)) >= N) {
                val (_D, x, y, u, v) =
                  if (o == 1) {
                    (2 * h - 1, s, t, a, b)
                  } else {
                    (2 * h, N - a, M - b, N - s, M - t)
                  }
                if (_D > 1 || (x != u && y != v)) {
                  val r1 = rec(ss1.take(x), ss2.take(y), i, j)
                  val r2 = rec(ss1.slice(u, N), ss2.slice(v, M), i + u, j + v)
                  return r1 ++ r2
                } else if (M > N) {
                  return rec(ss1.take(0), ss2.slice(N, M), i + N, j + N)
                } else if (M < N) {
                  return rec(ss1.slice(M, N), ss2.take(0), i + M, j + M)
                } else {
                  return Seq.empty
                }
              }
            }
          }
        }
        throw new RuntimeException("should never have reached here")
      } else if (N > 0) {
        if (N > 0) {
          (0 until N).map(n => Operation.Delete(from = i + n, count = 1))
        } else {
          Seq.empty
        }
      } else {
        if (M > 0) {
          (0 until M).map(m => Operation.Insert(at = i, from = j + m, count = 1)).toList
        } else {
          List.empty
        }
      }
    }
    rec(ss1, ss2, 0, 0)
  }

  sealed trait Operation extends Product with Serializable

  object Operation {
    case object Start                                       extends Operation
    final case class Insert(at: Int, from: Int, count: Int) extends Operation
    final case class Delete(from: Int, count: Int)          extends Operation
    case object End                                         extends Operation

  }

}
