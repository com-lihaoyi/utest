utest 0.0.1
===========

utest (pronounced micro-test) is a lightweight testing library for Scala. It's key features are:

- Very lightweight: utest is less than 1000 lines of code, far less than the [400,000 lines of code that make up ScalaTest](https://github.com/scalatest/scalatest/graphs/contributors) or the [50,000 lines that make up Specs2](https://github.com/etorreborre/specs2/graphs/contributors)
- [A fancy set of macro-powered asserts](#macro-asserts)
- [A unique execution model](#execution-model)
- [Integration with SBT](#running-tests-with-sbt)
- [Cross compiles to ScalaJS]()
- [Parallel testing]()

Defining and Running a Test Suite
=================================

```scala
val test = TestSuite{
  "test1"-{
    throw new Exception("test1")
  }
  "test2"-{
    1
  }
  "test3"-{
    val a = List[Byte](1, 2)
    a(10)
  }
}

// We have 4 tests in our test tree (3 leaves + the root)
println(test.toSeq.length) // 4
println(test.leaves.length) // 3

val results = test.run()

println(results.toSeq.length) // 4
println(results.leaves.length) // 3
println(results.leaves.count(_.value.isFailure)) // 2
println(results.leaves.count(_.value.isSuccess)) // 1
```

The simplest way to define a suite and start running tests is directly creating a test suite and running it programmatically. `TestSuite{}` returns a `Tree[Test]`, and the `.run()` method returns a `Tree[Result]`. `Tree[T]` is a n-ary tree which holds either the tests or the results.

As you can see, both `Test`s and `Result`s at the root of the tree are considered part of it. If you only want e.g. the `Result`s from the leaf tests, use the `.leaves` method to get an iterator for those.

Note that tests within the suite can nested within each other, but only directly. E.g. you cannot define tests within `if`-statements or `for`-loops. utest relies on the test structure to be statically known at compile time. They can be nested arbitrarily deep:

```scala
val test = TestSuite{
  val x = 1
  "outer"-{
    val y = x + 1
    "inner"-{
      val z = y + 1
      "innerest"-{
        assert(
          x == 1,
          y == 2,
          z == 3
        )
        (x, y, z)
      }
    }
  }
}
val results = test.run()
println(results.iterator.count(_.value.isSuccess) // 4
println(results.leaves.count(_.value.isSuccess)) // 1
```

Again, by default the results iterator includes the results of every node in the test tree, and you can use `.leaves` to only get the leaf nodes. Nesting is a convenient way of organizing related tests, and with the added bonus that you can place shared initialization code or helpers (e.g. the `val x`, `val y`, `val z` above) at the correct place within the tree where it is only visible to the tests that use it.

Despite it being shared lexically, these helpers are re-created for each test that is run, so if you if they contain mutable state (e.g. mutable collections, or `var`s) you do not need to worry about the mutations from multiple tests interfering with each other. For more detail on this and other things related to test execution, see [below](#execution-model).

Results
-------
```scala
test.run().toSeq.foreach(println)
// Result(Main$,Success(()), ...)
// Result(test1,Failure(java.lang.Exception: test1), ...)
// Result(test2,Success(1), ...)
// Result(test3,Failure(java.lang.IndexOutOfBoundsException: 10), ...)
```

`Result(name: String, value: Try[Any])` data structure is a simple data structure used to hold the results of the tests. Running the tests gives you a `Tree[Result]`, which is trivially convertible to a `Seq[Result]` and can be manipulated programmatically.

One of the more common things you want to do with `Result`s is print them out nicely so you can see what happened, and utest provides the `DefaultFormatter` class for exactly that purpose:

```scala

println(new DefaultFormatter().format(test.run()))

// Main$		Success
//     test1		Failure(java.lang.Exception: test1)
//     test2		Success(1)
//     test3		Failure(java.lang.IndexOutOfBoundsException: 10)
```

`DefaultFormatter` has a number of optional arguments that allow you to set the line-length-cutoff, whether the output uses console colors, whether stack traces are printed and other useful things.

You may have noticed that each `Result` contains a `Try[Any]` rather than an `Option[Throwable]`. This value is the last value in the test block, and is generally useful to pass data "out of" tests. For example, the `DefaultFormatter` displays it together with `Success` message, making it a nice sanity-check to let you confirm (visually) that the tests indeed did what they were supposed to.

Macro Asserts
=============

```scala
val x = 1
val y = "2"
assert(
  x > 0,
  x == y
)

// utest.LoggedAssertionError: Assert failed:
// x: Int = 1
// y: String = 2
```

utest comes with a macro-powered `assert`s that provide useful debugging information in the error message. These take one or more boolean expressions, and when they fail, will print out the names, types and values of any local variables used in the expression that failed. This makes it much easier to see what's going on than Scala's default `assert`, which gives you the stack trace and nothing else.

Intercept
---------

```scala
val e = intercept[MatchError]{
  (0: Any) match { case _: String => }
}
println(e)

// scala.MatchError: 0 (of class java.lang.Integer)
```

`intercept` allows you to verify that a block raises an exception. This exception is caught and returned so you can perform further validation on it, e.g. checking that the message is what you expect. If the block does not raise one, an `AssertionError` is raised.

Eventually and Continually
--------------------------

```scala
val x = Seq(12)
eventually(x == Nil)

// utest.LoggedAssertionError: Eventually failed:
// x: Seq[Int] = List(12)
```

In addition to a macro-powered `assert`, utest also provides macro-powered versions of `eventually` and `continually`. These are used to test asynchronous concurrent operations:

- `eventually(tests: Boolean*)`: ensure that the boolean values of `tests` all become true at least once within a certain period of time.
- `continually(tests: Boolean*)`: ensure that the boolean values of `tests` all remain true and never become false within a certain period of time.

These are implemented via a retry-loop, with a default retry interval of 0.1 second and retries up to a total of 1 second.

Together, these two operations allow you to easily test asynchronous operations. You can use them to help verify Liveness properties (that condition must eventually be met) and Safety properties (that a condition is never met)


Execution Model
===============

```scala
val test = TestSuite{
  var x = 0
  "A"-{
    x += 1
    "X"-{
      x += 2
      assert(x == 3)
    }
    "Y"-{
      x += 3
      assert(x == 4)
    }
  }
  "B"-{
    x += 4
    "Z"-{
      x += 5
      assert(x == 9)
    }
  }
}

val results = test.run()
println(results.leaves.count(_.value.isSuccess)) // 3
```

The example above demonstrates a subtlety of how utest suites are run: despite all the tests being able to refer to the same lexically-scoped value `x`, each tests modifications to `x` happen entirely independently of the others, allowing all three of the leaf-tests to pass. This allows you to easily place re-usable fixtures anywhere convenient within the test tree.

Running individual tests
------------------------
```scala
val tests = TestSuite{
  "A"-{
    "C"-1
  }
  "B"-{
    "D"-2
    "E"-3
  }
}


println(tests.run(testPath=Seq("A", "C")).toSeq))
// Seq(Result("C", Success(1), _, _))


println(tests.run(testPath=Seq("A")).toSeq)
// Seq(Result("A", Success(()), _, _), Result("C", Success(1), _, _))

println(tests.run(testPath=Seq("B")).toSeq)
// Seq(
//  Result("B", Success(()), _, _),
//  Result("D", Success(2), _, _),
//  Result("E", Success(3), _, _)
//)
```

You can run individual tests by passing a `testPath` argument to the `run` method. This allows you to specify which test or set of tests you want to run. When a test is selected, utest will run any tests in that subtree of the test tree, e.g. running the test `B` above also runs `D` and `E`.

If an outer test fails, all the tests in its subtree will be skipped:

```scala
var timesRun = 0

val tests = TestSuite{
  timesRun += 1
  "A"-{
    assert(false)
    "B"-{
      "C"-{
        1
      }
    }
  }
}

// inner tests aren't even tried, so only root
// and "A" are run, which is 2 runs
println(timesRun) // 2

println(res)
// Seq(
//   Result(_, Success(_), _, _),
//   Result("A", Failure(_: AssertionError), _, _),
//   Result("B", Failure(SkippedDueToOuterFailureError(Seq("A"), _: AssertionError)), _, _),
//   Result("C", Failure(SkippedDueToOuterFailureError(Seq("A"), _: AssertionError)), _, _)
// )
```

These inner tests are failed with `SkippedDueToOuterFailureError`.

Running tests with SBT
======================

To run tests using SBT, add the following to your `build.sbt` file:

```scala
libraryDependencies += "com.lihaoyi" % "utest_2.10" % "0.0.1"

testFrameworks += new TestFramework("utest.runner.Framework")
```

After that, you can use

```
sbt> test
```

To run all tests in your project. Tests are defined in any class inheriting from `TestSuite`, e.g.:

```scala
package mytests

object MyTestSuite extends TestSuite{
  val tests = TestSuite{
    "hello" - {
      "world" - { ... }
    }
    "test2" - { ... }
  }
}
```

The contents of the `tests` variable is identical to that shown [earlier](#defining-a-test-suite). If you want to run only a single `object` worth of tests (e.g. `MyTestSuite`) you can use:

```
sbt> test-only -- mytests.MyTestSuite
```

You can further drill down into a single test suite by providing the path to the test within it:

```
sbt> test-only -- mytests.MyTestSuite.hello.world
```

If you want to customize how the output is formatted, you can do so with command-line flags from SBT:

```
sbt> test-only -- mytests.MyTestSuite.hello.world --xxx=yyy
```

Where the values for `xxx` and `yyy` are:

- `--color=true` or `--color=false`: toggle console color output, defaults to `true`
- `--truncate=N`: cut off the printing of test results at `N` lines per result
- `--trace=true` or `--trace=false`: whether or not to print stack traces when things blow up, defaults to false.