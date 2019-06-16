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
    test{
      check(
        parse("hello"),
        Right(List(Tree("hello")))
      )
    }
    test{
      check(
        parse("\"hello\""),
        Right(List(Tree("hello")))
      )
    }
    test{
      check(
        parse("hello.world"),
        Right(List(Tree("hello", Tree("world"))))
      )
    }
    test{
      check(
        parse("""hello." w o r l d ".cow"""),
        Right(List(Tree("hello", Tree(" w o r l d ", Tree("cow")))))
      )
    }
    test{
      check(
        parse("hello.world.1"),
        Right(List(Tree("hello", Tree("world", Tree("1")))))
      )
    }
    test{
      check(
        parse("hello,world"),
        Right(List(Tree("hello"), Tree("world")))
      )
    }
    test{
      check(
        parse("{hello,world}"),
        Right(List(Tree("hello"), Tree("world")))
      )
    }

    test{
      check(
        parse("{{{hello.{{{{world}}}}}}}"),
        Right(List(Tree("hello", Tree("world"))))
      )
    }
    test{
      check(
        parse("foo.{bar,qux}"),
        Right(List(Tree("foo", Tree("bar"), Tree("qux"))))
      )
    }
    test{
      check(
        parse("foo.{bar.baz,qux}"),
        Right(List(Tree("foo", Tree("bar", Tree("baz")), Tree("qux"))))
      )
    }

    test{
      check(
        parse("foo.{bar.{baz},qux}"),
        Right(List(Tree("foo", Tree("bar", Tree("baz")), Tree("qux"))))
      )
    }
    test{
      check(
        parse("{foo.{bar.{baz.{qux}}}}"),
        Right(List(Tree("foo", Tree("bar", Tree("baz", Tree("qux"))))))
      )
    }
    test{
      check(
        parse("foo.bar.baz.qux"),
        Right(List(Tree("foo", Tree("bar", Tree("baz", Tree("qux"))))))
      )
    }
    test{
      check(
        parse("{foo.bar,foo.baz.qux,foo.baz.lol}"),
        Right(List(Tree("foo", Tree("bar"), Tree("baz", Tree("qux"), Tree("lol")))))
      )
    }
    test{
      check(
        parse("{test.utest.FrameworkTests,test.utest.QueryTests,test.utest.AssertsTests}"),
        Right(List(
          Tree("test", Tree("utest",
            Tree("FrameworkTests"),
            Tree("QueryTests"),
            Tree("AssertsTests")
          ))
        ))
      )
    }

    test{
      check(
        parse(""),
        Left("Unexpected end of input at index 0")
      )
    }
    test{
      check(
        parse("."),
        Left("Expected non-delimiter at index 0")
      )
    }
    test{
      check(
        parse("hello.{world"),
        Left("Expected closing bracket at index 12")
      )
    }
    test{
      check(
        parse("hello,"),
        Left("Expected end of input at index 5")
      )
    }
    test{
      check(
        parse("hello."),
        Left("Unexpected end of input at index 6")
      )
    }
    test{
      check(
        parse("hello}"),
        Left("Expected end of input at index 5")
      )
    }
    test{
      check(
        parse("\""),
        Left("Unclosed quote at index 0")
      )
    }
  }

}
