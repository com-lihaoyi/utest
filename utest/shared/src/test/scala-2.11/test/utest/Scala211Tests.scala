package test.utest
import utest._
object Foo extends TestSuite{
  val tests = Tests {
    'compileTimeOnly {
      // Make sure the 2.11 alias scala.annotation.compileTimeOnly works too,
      // in addition to the 2.10 scala.reflect.internal.annotations.compileTimeOnly
      compileError("compileTimeOnlyVal").check(
        """
      compileError("compileTimeOnlyVal").check(
                    ^
        """,
        "compileTimeOnlyVal should be a compile error if used!"
      )

    }
  }
  @scala.annotation.compileTimeOnly(
    "compileTimeOnlyVal should be a compile error if used!"
  )
  def compileTimeOnlyVal = 1
}