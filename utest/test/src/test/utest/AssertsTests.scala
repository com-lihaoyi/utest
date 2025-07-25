package test.utest
import utest._

/**
* Test suite for all the assertions that uTest comes bundled with.
*
* I use Predef.assert and manual try-catch-asserts throughout this suite,
* since it is the thing that is meant to be *testing* all the fancy uTest
* asserts, we can't assume they work.
*/
object AssertsTests extends utest.TestSuite{


  def tests = Tests{
    test("assert"){
      test("success"){
        def f(x: Boolean) = x
        assert(f(true))
        "success!"
      }
      test("failure"){
        val (e, logged, cause) = try {
          val x = 1
          val y = "2"
          assert(
            x > 0,
            x.toString == y
          )

          Predef.assert(false)
          ???
        } catch { case e @ utest.AssertionError(_, logged, cause) =>
          (e, logged, cause)
        }
        val expected = Seq(utest.TestValue("x", "Int", 1), TestValue("y", "String", "2"))
        test{
          Predef.assert(
            cause == null,
            "cause should be null for boolean failure"
          )
        }

        test{
          Predef.assert(
            logged == expected,
            "Logging didn't capture the locals properly " + logged
          )
          "hello" // make sure this works
        }

        test{
          val exText = fansi.Str(e.toString).plainText
          Predef.assert(
            exText.contains("y: String = \"2\"") && exText.contains("x: Int = 1"),
            "Logging doesn't display local values properly " + e.toString
          )
        }

        test{
          Predef.assert(
            e.toString.contains("x.toString == y"),
            "Message didnt contain source text " + e.toString
          )
        }
      }
      test("failureWithException"){
        try {
          assert(Iterator.empty.next() == 10)
          Predef.assert(false)
        } catch {case e @ utest.AssertionError(src, logged, cause) =>
          Predef.assert(cause.isInstanceOf[NoSuchElementException])
          Predef.assert(cause.getMessage == "next on empty iterator")
          e.getMessage
        }
      }

      test("tracingOnFailure"){
        try {
          val a = "i am cow"
          val b = 31337
          val c = 98
          assert(a + b == c.toString)
        } catch { case e: utest.AssertionError =>
          e.getMessage.contains("i am cow")
          e.getMessage.contains("31337")
          e.getMessage.contains("98")
        }
      }
      test("multiple"){
        def die = throw new IllegalArgumentException("foo")
        val msg1 = try {
          assert(
            1 == 2,
            die
          )
          ???
        } catch {case utest.AssertionError(msg, Nil, null) =>
          msg
        }
        Predef.assert(msg1.contains("#1: 1 == 2"))
        val msg2 = try {
          assert(
            1 == 1,
            die
          )
          ???
        } catch {case utest.AssertionError(msg, Nil, iae: IllegalArgumentException) =>
          msg + iae.getMessage
        }

        Predef.assert(msg2.contains("foo"))
        Predef.assert(msg2.contains("#2: die"))
      }
      test("show"){
        try assert((math.max(1 + 1, 2): @Show) == 3) catch{
          case utest.AssertionError(
            _,
            Seq(lv @ TestValue(_, "Int", 2)),
            null
          ) =>
            lv
        }
      }
    }
    test("arrowAssert"){
      1 ==> 1 // passes
      Array(1, 2, 3) ==> Array(1, 2, 3) // passes
      try{
        1 ==> 2 // throws
      }catch{case e: java.lang.AssertionError =>
        e
      }
    }
    test("intercept"){
      test("success"){
        val e = intercept[MatchError]{
          (0: Any) match { case _: String => }
        }
        Predef.assert(e.toString.contains("MatchError"))
        e.toString
      }
      test("failureWrongException"){
        try {
          val x = 1
          val y = 2.0
          intercept[NumberFormatException]{
            (x: Any) match { case _: String => y + 1 }
          }
          Predef.assert(false) // error wasn't thrown???
        } catch { case e: utest.AssertionError =>
          Predef.assert(e.getMessage.contains("(x: Any) match { case _: String => y + 1 }"))
          // This is subtle: only `x` should be logged as an interesting value, for
          // `y` was not evaluated at all and could not have played a part in the
          // throwing of the exception
          Predef.assert(e.captured == Seq(TestValue("x", "Int", 1)))
          Predef.assert(e.cause.isInstanceOf[MatchError])
          e.getMessage
        }
      }
      test("failureNoThrow"){
        try{
          val x = 1
          val y = 2.0
          intercept[NullPointerException]{
            123 + x + y
          }
        }catch {case e: utest.AssertionError =>
          Predef.assert(e.getMessage.contains("123 + x + y"))
          Predef.assert(e.captured == Seq(TestValue("x", "Int", 1), TestValue("y", "Double", 2.0)))
          e.getMessage
        }
      }
      test("interceptWithAssignment"){
        var W = 1
        try utest.intercept[Exception] { W = 2 }
        catch{case e: utest.AssertionError => e.getMessage}
      }
    }

    test("assertMatch"){
      test("success"){
        val thing = Seq(1, 2, 3)
        assertMatch(thing){case Seq(1, _, 3) =>}
        ()
      }
    }
    test("compileError"){
      test("success"){
        // Make sure that on successfully catching a compilation
        // error, the error it reports is in the correct place for
        // a variety of inputs
        val qq = "\"" * 3
        test - compileError("1 + abc").check(
          if (BuildInfo.scalaVersion.startsWith("3.")) """|1 + abc
                          |    ^  """.stripMargin
          else """
        test - compileError("1 + abc").check(
                                 ^
          """,
          if (BuildInfo.scalaVersion.startsWith("3.")) "Not found: abc"
          else "not found: value abc"
        )
        test - compileError(""" 1 + abc""").check(
          if (BuildInfo.scalaVersion.startsWith("3.")) """ 1 + abc
                         |     ^""".stripMargin
          else s"""
        test - compileError($qq 1 + abc$qq).check(
                                    ^
          """,
          if (BuildInfo.scalaVersion.startsWith("3.")) "Not found: abc"
          else "not found: value abc"
        )
        test - compileError("""
            1 + abc
          """).check(
          if (BuildInfo.scalaVersion.startsWith("3.")) """
            1 + abc
                ^""".tail
          else """
            1 + abc
                ^
          """,
          if (BuildInfo.scalaVersion.startsWith("3.")) "Not found: abc"
          else "not found: value abc"
        )
        test - compileError("""



            1 + abc


        """).check(
          if (BuildInfo.scalaVersion.startsWith("3.")) """
            1 + abc
                ^
          """.tail
          else """
            1 + abc
                ^

          """,
          if (BuildInfo.scalaVersion.startsWith("3.")) "Not found: abc"
          else "not found: value abc"
        )
        test - compileError("true * false").check(
          if (BuildInfo.scalaVersion.startsWith("3.")) """true * false
                         |     ^""".stripMargin
          else """
        test - compileError("true * false").check(
                                  ^
          """,
          "value * is not a member of Boolean"
        )
        // need to work around inability to use """ in string

        test - compileError(""" true * false""").check(
          if (BuildInfo.scalaVersion.startsWith("3.")) """ true * false
                         |      ^""".stripMargin
          else s"""
        test - compileError($qq true * false$qq).check(
                                     ^
          """,
          "value * is not a member of Boolean"
        )
        test - compileError("ab ( cd }").check(
          """""",
          if (BuildInfo.scalaVersion.startsWith("3.")) "')' expected, but '}' found"
          else "')' expected but '}' found."

        )
      }
    }
  }
}

