package test.utest

import utest.framework.Tree
import utest.runner.Query
import utest.runner.Query.parse
import utest._


object QueryTests extends utest.TestSuite{
  def check(a: Either[String, Query#Trees],
            b: Either[String, Query#Trees]) = {
    Predef.assert(a == b, a)
  }

  def tests = this{
    * - check(
      parse("hello"),
      Right(List(Tree("hello", Nil)))
    )
    * - check(
      parse("\"hello\""),
      Right(List(Tree("hello", Nil)))
    )
    * - check(
      parse("hello.world"),
      Right(List(Tree("hello", List(Tree("world", Nil)))))
    )
    * - check(
      parse("""hello." w o r l d ".cow"""),
      Right(List(Tree("hello", List(Tree(" w o r l d ", List(Tree("cow", Nil)))))))
    )
    * - check(
      parse("hello.world.1"),
      Right(List(Tree("hello", List(Tree("world", List(Tree("1", Nil)))))))
    )
    * - check(
      parse("hello,world"),
      Right(List(Tree("hello", Nil), Tree("world", Nil)))
    )
    * - check(
      parse("{hello,world}"),
      Right(List(Tree("hello", Nil), Tree("world", Nil)))
    )

    * - check(
      parse("{{{hello.{{{{world}}}}}}}"),
      Right(List(Tree("hello",List(Tree("world",Nil)))))
    )
    * - check(
      parse("foo.{bar,qux}"),
      Right(List(Tree("foo",List(Tree("bar",Nil), Tree("qux",Nil)))))
    )
    * - check(
      parse("foo.{bar.baz,qux}"),
      Right(List(Tree("foo",List(Tree("bar",List(Tree("baz", Nil))), Tree("qux",Nil)))))
    )

    * - check(
      parse("foo.{bar.{baz},qux}"),
      Right(List(Tree("foo",List(Tree("bar",List(Tree("baz", Nil))), Tree("qux",Nil)))))
    )
    * - check(
      parse("{foo.{bar.{baz.{qux}}}}"),
      Right(List(Tree("foo",List(Tree("bar",List(Tree("baz", List(Tree("qux",Nil)))))))))
    )
    * - check(
      parse("foo.bar.baz.qux"),
      Right(List(Tree("foo",List(Tree("bar",List(Tree("baz", List(Tree("qux",Nil)))))))))
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
