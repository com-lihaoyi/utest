package test.utest

import utest._
import utest.shaded.stringdiff._
import utest.shaded.stringdiff.DiffElement._

/**
 * Tests for the vendored stringdiff library.
 * Ported from https://github.com/tulz-app/stringdiff
 */
object StringDiffTests extends TestSuite {

  implicit class StringWithClean(s: String) {
    def clean: String = s.trim.replaceAll("\\s+", " ")
  }

  def tests = Tests {
    test("TokenDiff") {
      test("equal strings") {
        val result = TokenDiff.diff("diff-1 diff-2 diff-3", "diff-1 diff-2 diff-3")
        val expected = List(InBoth("diff-1 diff-2 diff-3"))
        assert(result == expected)
      }

      test("different prefixes") {
        val result = TokenDiff.diff(
          "prefix-1 match-1 match-2 match-3".clean,
          "prefix-2 match-1 match-2 match-3".clean
        )
        val expected = List(Diff("prefix-1", "prefix-2"), InBoth(" match-1 match-2 match-3"))
        assert(result == expected)
      }

      test("different suffixes") {
        val result = TokenDiff.diff(
          "match-1 match-2 match-3 suffix-1".clean,
          "match-1 match-2 match-3 suffix-2".clean
        )
        val expected = List(InBoth("match-1 match-2 match-3 "), Diff("suffix-1", "suffix-2"))
        assert(result == expected)
      }

      test("different token in the middle") {
        val result = TokenDiff.diff(
          "match-1 match-2 diff-1 match-3".clean,
          "match-1 match-2 diff-2 match-3".clean
        )
        val expected = List(InBoth("match-1 match-2 "), Diff("diff-1", "diff-2"), InBoth(" match-3"))
        assert(result == expected)
      }

      test("different prefix and suffix and token in the middle") {
        val result = TokenDiff.diff(
          "prefix-1 match-1 match-2 diff-1 match-3 suffix-1".clean,
          "prefix-2 match-1 match-2 diff-2 match-3 suffix-2".clean
        )
        val expected = List(
          Diff("prefix-1", "prefix-2"),
          InBoth(" match-1 match-2 "),
          Diff("diff-1", "diff-2"),
          InBoth(" match-3 "),
          Diff("suffix-1", "suffix-2")
        )
        assert(result == expected)
      }

      test("extra prefix in s1") {
        val result = TokenDiff.diff(
          "prefix-1 match-1 match-2 match-3".clean,
          "         match-1 match-2 match-3".clean
        )
        val expected = List(InFirst("prefix-1 "), InBoth("match-1 match-2 match-3"))
        assert(result == expected)
      }

      test("missing prefix in s1") {
        val result = TokenDiff.diff(
          "         match-1 match-2 match-3".clean,
          "prefix-1 match-1 match-2 match-3".clean
        )
        val expected = List(InSecond("prefix-1 "), InBoth("match-1 match-2 match-3"))
        assert(result == expected)
      }

      test("extra token in s1") {
        val result = TokenDiff.diff(
          "match-1 diff-1 match-2".clean,
          "match-1        match-2".clean
        )
        val expected = List(InBoth("match-1"), InFirst(" diff-1"), InBoth(" match-2"))
        assert(result == expected)
      }

      test("extra token in s2") {
        val result = TokenDiff.diff(
          "match-1        match-2".clean,
          "match-1 diff-1 match-2".clean
        )
        val expected = List(InBoth("match-1"), InSecond(" diff-1"), InBoth(" match-2"))
        assert(result == expected)
      }

      test("two extra tokens in s1") {
        val result = TokenDiff.diff(
          "match-1 diff-1 diff-2 match-2".clean,
          "match-1               match-2".clean
        )
        val expected = List(InBoth("match-1"), InFirst(" diff-1 diff-2"), InBoth(" match-2"))
        assert(result == expected)
      }

      test("two extra tokens in s2") {
        val result = TokenDiff.diff(
          "match-1               match-2".clean,
          "match-1 diff-1 diff-2 match-2".clean
        )
        val expected = List(InBoth("match-1"), InSecond(" diff-1 diff-2"), InBoth(" match-2"))
        assert(result == expected)
      }

      test("extra suffix in s1") {
        val result = TokenDiff.diff(
          "match-1 match-2 match-3 suffix-1".clean,
          "match-1 match-2 match-3         ".clean
        )
        val expected = List(InBoth("match-1 match-2 match-3"), InFirst(" suffix-1"))
        assert(result == expected)
      }

      test("extra suffix in s2") {
        val result = TokenDiff.diff(
          "match-1 match-2 match-3        ".clean,
          "match-1 match-2 match-3 suffix-1".clean
        )
        val expected = List(InBoth("match-1 match-2 match-3"), InSecond(" suffix-1"))
        assert(result == expected)
      }

      test("extra prefix and suffix in s1") {
        val result = TokenDiff.diff(
          "prefix-1 match-1 match-2 match-3 suffix-1".clean,
          "         match-1 match-2 match-3         ".clean
        )
        val expected = List(InFirst("prefix-1 "), InBoth("match-1 match-2 match-3"), InFirst(" suffix-1"))
        assert(result == expected)
      }

      test("extra prefix and suffix in s2") {
        val result = TokenDiff.diff(
          "         match-1 match-2 match-3         ".clean,
          "prefix-1 match-1 match-2 match-3 suffix-1".clean
        )
        val expected = List(InSecond("prefix-1 "), InBoth("match-1 match-2 match-3"), InSecond(" suffix-1"))
        assert(result == expected)
      }
    }

    test("StringDiff") {
      test("equal strings") {
        val result = StringDiff.diff("hello world", "hello world")
        val expected = List(InBoth("hello world"))
        assert(result == expected)
      }

      test("different strings") {
        val result = StringDiff.diff("hello", "world")
        // The diff algorithm finds the common 'o' character
        assert(result.nonEmpty)
        assert(result.exists(e => e.isInstanceOf[Diff[_]] || e.isInstanceOf[InFirst[_]] || e.isInstanceOf[InSecond[_]]))
      }

      test("partial match") {
        val result = StringDiff.diff("hello world", "hello there")
        assert(result.exists(_.isInstanceOf[InBoth[_]]))
        assert(result.exists(e => e.isInstanceOf[Diff[_]] || e.isInstanceOf[InFirst[_]] || e.isInstanceOf[InSecond[_]]))
      }
    }

    test("SeqDiff") {
      test("equal sequences") {
        val result = SeqDiff.seq(Vector(1, 2, 3), Vector(1, 2, 3))
        val expected = List(InBoth(Vector(1, 2, 3)))
        assert(result == expected)
      }

      test("different sequences") {
        val result = SeqDiff.seq(Vector(1, 2, 3), Vector(4, 5, 6))
        // Completely different sequences produce a Diff element
        assert(result.nonEmpty)
        assert(result.exists(e => e.isInstanceOf[Diff[_]] || e.isInstanceOf[InFirst[_]] || e.isInstanceOf[InSecond[_]]))
      }

      test("partial match") {
        val result = SeqDiff.seq(Vector(1, 2, 3, 4), Vector(1, 2, 5, 4))
        assert(result.exists(_.isInstanceOf[InBoth[_]]))
      }
    }

    test("Performance") {
      test("large sequence diff completes quickly") {
        val start = System.currentTimeMillis()
        val s1 = (0 until 500).toVector
        val s2 = Vector.empty[Int]
        val result = SeqDiff.seq(s1, s2)
        val elapsed = System.currentTimeMillis() - start

        // Should complete in well under 10 seconds
        assert(elapsed < 10000)
        // Result should show s1 content as "in first only"
        assert(result.exists(_.isInstanceOf[InFirst[_]]))
      }

      test("large string diff completes quickly") {
        val start = System.currentTimeMillis()
        val s1 = (0 until 500).mkString(" ")
        val s2 = ""
        val result = StringDiff.diff(s1, s2)
        val elapsed = System.currentTimeMillis() - start

        // Should complete in well under 10 seconds
        assert(elapsed < 10000)
        assert(result.nonEmpty)
      }
    }
  }
}
