package test.utest

import utest.framework.Tree
import utest.TestQueryParser
import utest.TestQueryParser.parse
import utest._


object QueryTests extends utest.TestSuite{
  def check(a: Either[String, TestQueryParser#Trees],
            b: Either[String, TestQueryParser#Trees]) = {
    Predef.assert(a == b, a)
  }

  def tests = Tests{
    * - check(
      parse("hello"),
      Right(List(Tree("hello")))
    )
    * - check(
      parse("\"hello\""),
      Right(List(Tree("hello")))
    )
    * - check(
      parse("hello.world"),
      Right(List(Tree("hello", Tree("world"))))
    )
    * - check(
      parse("""hello." w o r l d ".cow"""),
      Right(List(Tree("hello", Tree(" w o r l d ", Tree("cow")))))
    )
    * - check(
      parse("hello.world.1"),
      Right(List(Tree("hello", Tree("world", Tree("1")))))
    )
    * - check(
      parse("hello,world"),
      Right(List(Tree("hello"), Tree("world")))
    )
    * - check(
      parse("{hello,world}"),
      Right(List(Tree("hello"), Tree("world")))
    )

    * - check(
      parse("{{{hello.{{{{world}}}}}}}"),
      Right(List(Tree("hello", Tree("world"))))
    )
    * - check(
      parse("foo.{bar,qux}"),
      Right(List(Tree("foo", Tree("bar"), Tree("qux"))))
    )
    * - check(
      parse("foo.{bar.baz,qux}"),
      Right(List(Tree("foo", Tree("bar", Tree("baz")), Tree("qux"))))
    )

    * - check(
      parse("foo.{bar.{baz},qux}"),
      Right(List(Tree("foo", Tree("bar", Tree("baz")), Tree("qux"))))
    )
    * - check(
      parse("{foo.{bar.{baz.{qux}}}}"),
      Right(List(Tree("foo", Tree("bar", Tree("baz", Tree("qux"))))))
    )
    * - check(
      parse("foo.bar.baz.qux"),
      Right(List(Tree("foo", Tree("bar", Tree("baz", Tree("qux"))))))
    )
    * - check(
      parse("{foo.bar,foo.baz.qux,foo.baz.lol}"),
      Right(List(Tree("foo", Tree("bar"), Tree("baz", Tree("qux"), Tree("lol")))))
    )
    * - check(
      parse("{test.utest.FrameworkTests,test.utest.QueryTests,test.utest.AssertsTests}"),
      Right(List(
        Tree("test", Tree("utest",
          Tree("FrameworkTests"),
          Tree("QueryTests"),
          Tree("AssertsTests")
        ))
      ))
    )

    * - check(
      parse(""),
      Left("Unexpected end of input at index 0")
    )
    * - check(
      parse("."),
      Left("Expected non-delimiter at index 0")
    )
    * - check(
      parse("hello.{world"),
      Left("Expected closing bracket at index 12")
    )
    * - check(
      parse("hello,"),
      Left("Expected end of input at index 5")
    )
    * - check(
      parse("hello."),
      Left("Unexpected end of input at index 6")
    )
    * - check(
      parse("hello}"),
      Left("Expected end of input at index 5")
    )
    * - check(
      parse("\""),
      Left("Unclosed quote at index 0")
    )
  }

}
