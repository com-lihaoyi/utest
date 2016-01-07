µTest 0.3.1
===========

uTest (pronounced micro-test) is a lightweight testing library for Scala. Its key features are:

- [Less than 1000 lines of code](https://github.com/lihaoyi/utest/graphs/contributors)
- [A fancy set of macro-powered asserts](#macro-asserts)
- [A unique execution model](#execution-model)
- [Integration with SBT](#running-tests-with-sbt)
- [Cross compiles to ScalaJS](#scalajs)
- [Parallel testing](#parallel-testing)

Contents
--------

- [Getting Started](#getting-started)
- [Defining and Running a Test Suite](#defining-and-running-a-test-suite)
  - [Results](#results)
  - [Asynchronous Tests](#asynchronous-tests)
- [Macro Asserts](#macro-asserts)
  - [Intercept](#intercept)
  - [Eventually and Continually](#eventually-and-continually)
  - [Assert Match](#assert-match)
  - [Compile Error](#compile-error)
- [Execution Model](#execution-model)
- [Test running API](#test-running-api)
  - [Parallel Testing](#parallel-testing)
- [Running tests with SBT](#running-tests-with-sbt)
- [ScalaJS](#scalajs)
  - [ScalaJS and SBT](#scalajs-and-sbt)
  - [JsCrossBuild](#jscrossbuild)
- [Why uTest](#why-utest)

Getting Started
===============

```scala
libraryDependencies += "com.lihaoyi" %% "utest" % "0.3.1"
```

Add the following to your `build.sbt` and you can immediately begin defining and running tests programmatically. [Continue reading](#defining-and-running-a-test-suite) to see how to define and run your test suites, or jump to [Running tests with SBT](#running-tests-with-sbt) to find out how to mark and run your test suites from the SBT console.

Defining and Running a Test Suite
=================================

```scala
import utest._
import utest.ExecutionContext.RunNow

val test = TestSuite{
  'test1{
    throw new Exception("test1")
  }
  'test2{
    1
  }
  'test3{
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

The simplest way to define a suite and start running tests is directly creating a test suite and running it programmatically. `TestSuite{}` returns a `Tree[Test]`, and the `.run()` method returns a `Tree[Result]`. `Tree[T]` is a n-ary tree which holds either the tests or the results. Importing `ExecutionContext.RunNow` indicates that you want tests to be run without parallelism on the current thread.

As you can see, both `Test`s and `Result`s at the root of the tree are considered part of it. If you only want e.g. the `Result`s from the leaf tests, use the `.leaves` method to get an iterator for those.

Note that tests within the suite can nested within each other, but only directly. E.g. you cannot define tests within `if`-statements or `for`-loops. uTest relies on the test structure to be statically known at compile time. They can be nested arbitrarily deep:

```scala
val test = TestSuite{
  val x = 1
  'outer{
    val y = x + 1
    'inner{
      val z = y + 1
      'innerest{
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
println(results.iterator.count(_.value.isSuccess)) // 4
println(results.leaves.count(_.value.isSuccess)) // 1
```

Again, by default the `results` iterator includes the results of every node in the test tree, and you can use `.leaves` to only get the leaf nodes. Nesting is a convenient way of organizing related tests, and with the added bonus that you can place shared initialization code or helpers (e.g. the `val x`, `val y`, `val z` above) at the correct place within the tree where it is only visible to the tests that use it.

Despite being shared lexically, these helpers are re-created for each test that is run, so if they contain mutable state (e.g. mutable collections, or `var`s) you do not need to worry about the mutations from multiple tests interfering with each other. For more detail on this and other things related to test execution, see [below](#execution-model).

uTest also allows you to use strings to define test keys, if you wish to make your test names longer and more descriptive:

```scala
TestSuite{
  "test with spaces"-{
    throw new Exception("test1")
  }
  'test2-run(1)
}
```

Note that you can also use the `'symbol-...` syntax, if your tests are concise and you want to make them *really* concise. The `"string"-{...}`, `'symbol{...}` and `'symbol-...` syntaxes are all entirely equivalent.

The last way of defining tests is with the `utest.*` symbol, e.g.

```scala
import utest._

val test = TestSuite{
  'test1{
    throw new Exception("test1")
  }
  'test2{
    * - {1 == 1}
    * - {2 == 2}
    * - {3 == 3}
  }
  'test3{
    val a = List[Byte](1, 2)
    a(10)
  }
}
```

Tests defined using the `*` symbol are give the numerical names "0", "1", "2", etc.. This is handy if you have a very large number of very simple test cases (perhaps you've delegated the heavy lifting to a helper function), but still want to be able to run them separately.  

Results
-------
```scala
test.run().toSeq.foreach(println)
// Result(Main$,Success(()), ...)
// Result(test1,Failure(java.lang.Exception: test1), ...)
// Result(test2,Success(1), ...)
// Result(test3,Failure(java.lang.IndexOutOfBoundsException: 10), ...)
```

The `Result(name: String, value: Try[Any])` data structure is a simple data structure used to hold the results of the tests. Running the tests gives you a `Tree[Result]`, which is trivially convertible to a `Seq[Result]` and can be manipulated programmatically.

One of the more common things you want to do with `Result`s is print them out nicely so you can see what happened, and uTest provides the `DefaultFormatter` class for exactly that purpose:

```scala

println(new DefaultFormatter().format(test.run()))

// Main$		Success
//     test1		Failure(java.lang.Exception: test1)
//     test2		Success(1)
//     test3		Failure(java.lang.IndexOutOfBoundsException: 10)
```

`DefaultFormatter` has a number of optional arguments that allow you to set the line-length-cutoff, whether the output uses console colors, whether stack traces are printed and other useful things.

You may have noticed that each `Result` contains a `Try[Any]` rather than an `Option[Throwable]`. This value is the last value in the test block, and is generally useful to pass data "out of" tests. For example, the `DefaultFormatter` displays it together with `Success` message, making it a nice sanity-check to let you confirm (visually) that the tests indeed did what they were supposed to.

By default, uTest now only reports test results. To enable (past behavior of) logging test progress as they run, you can pass the `--progress` flag.

Asynchronous Tests
------------------

```scala
val tests = TestSuite {
  "testSuccess" - {
    Future {
      assert(true)
    }
  }
  "testFail" - {
    Future {
      assert(false)
    }
  }
  "normalSuccess" - {
    assert(true)
  }
  "normalFail" - {
    assert(false)
  }
}

tests.runAsync().map {    results =>
  assert(results.toSeq(0).value.isSuccess) // root
  assert(results.toSeq(1).value.isSuccess) // testSuccess
  assert(results.toSeq(2).value.isFailure) // testFail
  assert(results.toSeq(3).value.isSuccess) // normalSuccess
}
```

You can have tests which return (have a last expression being) a `Future[T]` instead of a normal value. You can run the suite using `.runAsync` to return a `Future` of the results, or you can continue using `.run` which will wait for all the futures to complete before returning.

In Scala.js, calling `.run` on a test suite with futures in it throws an error instead of waiting, since you cannot wait in Scala.js.

When running the test suites from SBT, you do not need worry about any of this `run` vs `runAsync` stuff: the test runner will handle it for you and provide the correct results. 

Macro Asserts
=============

```scala
val x = 1
val y = "2"
assert(
  x > 0,
  x == y
)

// utest.AssertionError: x == y
// x: Int = 1
// y: String = 2
```

uTest comes with a macro-powered `assert`s that provide useful debugging information in the error message. These take one or more boolean expressions, and when they fail, will print out the names, types and values of any local variables used in the expression that failed. This makes it much easier to see what's going on than Scala's default `assert`, which gives you the stack trace and nothing else.

uTest also wraps any exceptions thrown within the assert, to help trace what went wrong:

```scala
val x = 1L
val y = 0L
assert(x / y == 10)

// utest.AssertionError: assert(x / y == 10)
// caused by: java.lang.ArithmeticException: / by zero
// x: Long = 1
// y: Long = 0
```

The origin exception is stored as the `cause` of the `utest.AssertionError`, so the original stack trace is still available for you to inspect.

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

As with `assert`, `intercept` adds debugging information to the error messages if the `intercept` fails or throws an unexpected Exception.

Eventually and Continually
--------------------------

```scala
val x = Seq(12)
eventually(x == Nil)

// utest.AssertionError: eventually(x == Nil)
// x: Seq[Int] = List(12)
```

In addition to a macro-powered `assert`, uTest also provides macro-powered versions of `eventually` and `continually`. These are used to test asynchronous concurrent operations:

- `eventually(tests: Boolean*)`: ensure that the boolean values of `tests` all become true at least once within a certain period of time.
- `continually(tests: Boolean*)`: ensure that the boolean values of `tests` all remain true and never become false within a certain period of time.

These are implemented via a retry-loop, with a default retry interval of 0.1 second and retries up to a total of 1 second. If you want to change this behavior, you can shadow the implicit values `retryInterval` and `retryMax`, for example this:

```scala
implicit val retryMax = RetryMax(300.millis)
implicit val retryInterval = RetryInterval(50.millis)
```

Would set the retry-loop to happen every 50ms up to a max of 300ms.

Together, these two operations allow you to easily test asynchronous operations. You can use them to help verify Liveness properties (that condition must eventually be met) and Safety properties (that a condition is never met)

As with `assert`, `eventually` and `continually` add debugging information to the error messages if they fail.

Assert Match
------------

```scala
assertMatch(Seq(1, 2, 3)){case Seq(1, 2) =>}
// AssertionError: Matching failed Seq(1, 2, 3)
```

`assertMatch` is a convenient way of checking that a value matches a particular shape, using Scala's pattern matching syntax. This includes support for use of `|` or `_` or `if`-guards within the pattern match. This gives you additional flexibility over a traditional `assert(a == Seq(1, 2))`, as you can use `_` as a wildcard e.g. using `assertMatch(a){case Seq(1, _)=>}` to match any 2-item `Seq` whose first item is `1`.

As with `assert`, `assertMatch` adds debugging information to the error messages if the value fails to match or throws an unexpected Exception while evaluating.

Compile Error
-------------

```scala
compileError("true * false")
// CompileError.Type("value * is not a member of Boolean")

compileError("(}")
// CompileError.Parse("')' expected but '}' found.")
```

`compileError` is a macro that can be used to assert that a fragment of code (given as a literal String) fails to compile. 

- If the code compiles successfully, `compileError` will fail the compilation run with a message.
- If the code fails to compile, `compileError` will return an instance of `CompileError`, one of `CompileError.Type(pos: String, msgs: String*)` or `CompileError.Parse(pos: String, msgs: String*)` to represent typechecker errors or parser errors
 
In general, `compileError` works similarly to `intercept`, except it does its checks (that a snippet of code fails) and errors (if it doesn't fail) at compile-time rather than run-time. If the code fails as expected, the failure message is propagated to runtime in the form of a `CompileError` object. You can then do whatever additional checks you want on the failure message, such as verifying that the failure message contains some string you expect to be there.

The `compileError` macro compiles the given string in the local scope and context. This means that you can refer to variables in the enclosing scope, i.e. the following example will fail to compile because the variable `x` exists.  

```scala
val x = 0

compileError("x + x"),
// [error] compileError check failed to have a compilation error
```

The returned `CompileError` object also has a handy `.check` method, which takes a position-string indicating where the error is expected to occur, as well as zero-or-more messages which are expected to be part of the final error message. This is used as follows:

```scala
    compileError("true * false").check(
      """
    compileError("true * false").check(
                       ^
      """,
      "value * is not a member of Boolean"
    )
```

Note that the position-string needs to exactly match the line of code the compile-error occured on. This includes any whitespace on the left, as well as any unrelated code or comments sharing the same line as the `compileError` expression.  

Execution Model
===============

```scala
val test = TestSuite{
  var x = 0
  'A{
    x += 1
    'X{
      x += 2
      assert(x == 3)
    }
    'Y{
      x += 3
      assert(x == 4)
    }
  }
  'B{
    x += 4
    'Z{
      x += 5
      assert(x == 9)
    }
  }
}

val results = test.run()
println(results.leaves.count(_.value.isSuccess)) // 3
```

The example above demonstrates a subtlety of how uTest suites are run: despite all the tests being able to refer to the same lexically-scoped value `x`, each tests modifications to `x` happen entirely independently of the others, allowing all three of the leaf-tests to pass. This allows you to easily place re-usable fixtures anywhere convenient within the test tree.

If you want to create a shared resource that is lazily initialized when needed in one of the tests and used throughout them, simply make it a `lazy val` outside the `TestSuite{ ... }` block.

Test-discovery is done entirely at compile-time by the `TestSuite{ ... }` macro, and is independent of execution of the tests:

```scala
val tests = TestSuite{
  timesRun += 1
  'A{
    assert(false)
    'B{
      'C{
        1
      }
    }
  }
}

// listing tests B and C works despite failure of A
println(tests.toSeq.map(_.name)) // Seq(_, A, B, C)
```

As you can see, even though the `assert(false)` comes before the declaration of tests `B` and `C`, these tests are still listable and inspectable because they were registered at compile time using the `TestSuite{ ... }` macro.

Having a clean separation between test-discovery and test-execution is generally considered to be a good thing, and uTest's execution model strictly enforces this by doing test-discovery at compile-time. Thus, you can always inspect the test hierarchy without having to execute arbitrary test code. At the same time, uTest preserves all the convenience of sharing common setup code via lexical scoping, while avoiding the pitfall of shared-state between tests, giving you the best of both worlds in terms of convenience and isolation.

Test running API
================

This section goes into the Scala API for running a uTest suite. For the SBT command-line API, see [Running tests with SBT](#running-tests-with-sbt).

```scala
val tests = TestSuite{
  'A{
    'C-1
  }
  'B{
    'D-2
    'E-3
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

You can run individual tests by passing a `testPath` argument to the `run` method. This allows you to specify which test or set of tests you want to run. When a test is selected, uTest will run any tests in that subtree of the test tree, e.g. running the test `B` above also runs `D` and `E`.

If an outer test fails, all the tests in its subtree will be skipped:

```scala
var timesRun = 0

val tests = TestSuite{
  timesRun += 1
  'A{
    assert(false)
    'B{
      'C{
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

Parallel Testing
----------------

If you want to run your tests in parallel, simply replace

```scala
import utest.ExecutionContext.RunNow
```

With

```scala
import ExecutionContext.Implicits.global
```

That will cause your tests to be distributed on your default `global` ExecutionContext, which parallelizes it on a global ForkJoinPool to make use of all your cores. Note that even without using a parallelizing `ExecutionContext`, SBT will run separate suites in parallel. The parallel `ExecutionContext` also does not work with ScalaJS

Running tests with SBT
======================

To run tests using SBT, add the following to your `build.sbt` file:

```scala
libraryDependencies += "com.lihaoyi" %% "utest" % "0.3.1"

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
    'hello{
      'world{
        val x = 1
        val y = 2
        assert(x != y)
        (x, y)
      }
    }
    'test2{
      val a = 1
      val b = 2
      assert(a == b)
    }
  }
}
```

Running:

```
sbt> test-only -- mytests.MyTestSuite
```

Will print out something like this:

```
[info] 1/4     utest.MyTestSuite.hello.world		Success((1,2))
[info] 2/4     utest.MyTestSuite.hello		Success
[info] 3/4     utest.MyTestSuite.test2		Failure(utest.AssertionError: assert(a == b)
[info] ...
[info] 4/4     utest.MyTestSuite.		Success
[info] -----------------------------------Results-----------------------------------
[info] MyTestSuite$		Success
[info]     hello		Success
[info]         world		Success((1,2))
[info]     test2		Failure(utest.AssertionError: assert(a == b)
[info]     ...
[info] Tests: 4
[info] Passed: 3
[info] Failed: 1
```

As you can see, the total number of tests includes the non-leaf tests like `hello` and `MyTestSuite`. Also, the tests which return a value (like `world`) have that value printed out inside the `Success()` tag: this is handy for doing a visual sanity-check at the end of the run to make sure the tests are doing what you think they are

You can further drill down into a single test suite by providing the path to the test within it:

```
sbt> test-only -- mytests.MyTestSuite.test2
```


Which will only run one test.

```
[info] 1/4     utest.MyTestSuite.		Failure(utest.AssertionError: assert(a == b)
[info] ...
[info] -----------------------------------Results-----------------------------------
[info] test2		Failure(utest.AssertionError: assert(a == b)
[info] ...
[info] Tests: 1
[info] Passed: 0
[info] Failed: 1

```
If you want to customize how the output is formatted, you can do so with command-line flags from SBT, for example to turn on tracing

```
sbt> test-only -- mytests.MyTestSuite.test2 --trace=true
```

Which shows you the exception that gets raised:

```
[info] 1/4     utest.MyTestSuite.		Failure(utest.AssertionError: assert(a == b)
[info] ...
[info] utest.AssertionError: assert(a == b)
[info] a: Int = 1
[info] b: Int = 2
...
[info] 	at utest.MyTestSuite$$anonfun$4$$anonfun$apply$4.apply(Main.scala:30)
[info] 	at utest.MyTestSuite$$anonfun$4$$anonfun$apply$4.apply(Main.scala:18)
...
[info] -----------------------------------Results-----------------------------------
[info] test2		Failure(utest.AssertionError: assert(a == b)
[info] ...
[info] Tests: 1
[info] Passed: 0
[info] Failed: 1
```

Other flags you can pass to the test suite (when invoked by `test-only`) are:

- `--color=true` or `--color=false`: toggle console color output, defaults to `true`
- `--truncate=N`: cut off the printing of test results at `N` lines per result
- `--trace=true` or `--trace=false`: whether or not to print stack traces when things blow up, defaults to false.
- `--parallel=true` or `--parallel=false`: whether tests within a `TestSuite` should be run in parallel, defaults to `false`. Tests in different suites are always run task-parallel by SBT
- `--progress=true`: enable past behavior of logging progress messages as each test is run, defaults to `false` and the `=true` suffix can be omitted. 

ScalaJS
=======

uTest is completely compatible with ScalaJS: the above sections on defining a test suite, asserts and the test-running API all work unchanged under ScalaJS, with minor differences:

- ScalaJS does not support parallelism, and as such only single-threaded `ExecutionContexts` like `utest.ExecutionContext.runNow` or `scala.scalajs.concurrent.JSExecutionContext.runNow` work. When run via SBT, `--parallel` has no effect.
- [eventually](#eventually) and [continually](#continually) are not supported, as they rely on a blocking retry-loop whereas you can't block in ScalaJS

Apart from these differences, there should be no problem compiling uTest TestSuites via ScalaJS and running them using Rhino or in the browser.

ScalaJS and SBT
---------------

To get SBT to run your uTest suites under ScalaJS, add the following to your `build.sbt`:

```scala
libraryDependencies += "com.lihaoyi" %%% "utest" % "0.3.1"

testFrameworks += new TestFramework("utest.runner.Framework")
```

Note that your project must already be a ScalaJS project. With these snippets set up, all of the commands described in [Running tests with SBT](#running-tests-with-sbt) should behave identically, except that your test suites will be compiled to Javascript and run in ScalaJS's `JsEnv`, instead of on the JVM. By default this is Rhino, but it can be configured to use NodeJS or PhantomJS if you have them installed. Test selection, coloring, etc. should all work unchanged.

uTest 0.3.1 is compatible with ScalaJS 0.6.x

Cross Building
--------------

uTest's own support for cross building projects (via `JsCrossBuild`) has been removed in 0.3.0. Instead, use Scala.js' own support for cross-building via `crossProject`

Why uTest
=========

uTest began as an attempt to port [ScalaTest](http://www.scalatest.org/) and [Specs2](http://etorreborre.github.io/specs2/) to [ScalaJS](). After struggling with that, I realized that both ScalaTest and Specs2 were going to be difficult to port to ScalaJS for a few reasons:

- They have a large number of dependencies on JVM-specific things, such as [Symbols or Reflection](http://www.scalatest.org/user_guide/using_matchers#checkingArbitraryProperties) or [ClassLoaders](), which are not supported by ScalaJS
- They have huge codebases: [400,000 lines of code for ScalaTest](https://github.com/scalatest/scalatest/graphs/contributors) and [50,000 lines for Specs2](https://github.com/etorreborre/specs2/graphs/contributors). This huge mass of code makes it difficult to pinpoint the parts which are incompatible with ScalaJS.

If you don't believe that uTest is much smaller than the alternatives, let the jars speak for themselves:

![Sizes.png](Sizes.png?raw=true)

uTest tries to provide most of what you want as a developer, while leaving out all the unnecessary functionality that ScalaTest and Specs2 provide:

- Fluent English-like code: matchers like [`shouldBe` or `should not be`](http://www.scalatest.org/user_guide/using_matchers#checkingForEmptiness) or [`mustbe_==`](http://etorreborre.github.io/specs2/guide/org.specs2.guide.Matchers.html) don't really add anything, and it doesn't really matter whether you name each test block using [`should`, `when`, `can`, `must`](http://doc.scalatest.org/2.0/#org.scalatest.Spec), [`feature("...")`](http://doc.scalatest.org/2.0/#org.scalatest.FlatSpec) or [`it should "..."`](http://doc.scalatest.org/2.0/#org.scalatest.FlatSpec) These add nothing and clutter up the API and code base. You certainly don't need [8 different sets of them](http://www.scalatest.org/user_guide/selecting_a_style).
- Legacy code, like ScalaTests [time package](http://doc.scalatest.org/2.0/#org.scalatest.time.package), now obsolete with the introduction of [scala.concurrent.duration](http://www.scala-lang.org/api/current/index.html#scala.concurrent.duration.package).
- Such a a rich command-line interface: with a simple API, any user who wants to do heavy customization of the test running can simply do it in code, and writing a small amount of Scala with a trivial command-line runner will likely be easier than wrestling with mountains of command-line configuration flags to try to make the runner do what you want.

While improving on the basic things that matters

- Better [macro-asserts](#macro-assserts) which are both more-useful and more-simply-implemented than those provided by ScalaTest
- Compile-time test registration, which allows [completely separating test-discovery and execution](#execution-model).
- A simpler, straightforward [API](#test-running-api) that makes using uTest as a library much easier.
- Raw size: at less than 1000 lines of code, uTest is 1/400th the size of [ScalaTest](https://github.com/scalatest/scalatest/graphs/contributors) and 1/50th the size of [Specs2](https://github.com/etorreborre/specs2/graphs/contributors), and with almost no dependencies. Its small size means that you can trivially use uTest as a library within a larger application without worrying about it significantly increasing the size of your packaged artifacts, or pulling in weird dependencies.

Development Tips
================

To run all the test on the entire matrix of Scala versions (2.10.4 and 2.11.0) and backends (JVM and JS), simply run:

    sbt +test

You can also use more targeted commands e.g. `utestJS/test` which would only re-test the Javascript backend under scala 2.10.4.

To publish use

    sbt +publishSigned

Changelog
=========

0.3.1
-----

- Published for Scala.js 0.6.1

0.3.0
-----

- Published for Scala.js 0.6.0
- Removed `JsCrossBuild` now that Scala.js supports cross-building via `crossProject`
- `compileTimeOnly` has been re-introduced, so invalid use of test DSL should fail with nice errors
- Removed `--throw` flag in favor of "native" SBT error reporting

0.2.4
-----

- Added support for asynchronous tests which return a `Future`. 

0.2.3
-----

- Updated to work against ScalaJS 0.5.4

0.2.2
-----

- Introduced `CompileError.check(pos: String, msgs: String*)` to simplify the common pattern of checking that the error occurs in the right place and with the message you expect.
- Changed the file layout expected by `JsCrossBuild`, to expect the shared files to be in `js/shared/` and `jvm/shared/`, rather than in `shared/`. This is typically achieved via symlinks, and should make the cross-build play much more nicely with IDEs.

0.2.1
-----

- Fix bug in `utestJsSettings` pulling in the wrong version of uTest

0.2.0
-----
- Introduced the `compileError` macro to allow testing of compilation errors.
- Stack traces are now only shown for the user code, with the uTest/SBT internal stack trace ignored, making them much less spammy and noisy.
- Introduced the `*` symbol, which can be used in place of a test name to get sequentially numbered test names.

0.1.9
-----


- ScalaJS version is now built against ScalaJS 0.5.3
- Fixed linking errors in ScalaJS version, to allow proper operation of the new optimization

0.1.8
-----

- Fixed bug causing local-defs in assert macro to fail
 
0.1.7
-----

- Extracted out `utestJvmSettings` and `utestJsSettings` for use outside the `JsCrossBuild` plugin, for people who don't want to use the plugin.
 
0.1.6
-----

- Print paths of failing tests after completion to make C&P-ing re-runs more convenient
