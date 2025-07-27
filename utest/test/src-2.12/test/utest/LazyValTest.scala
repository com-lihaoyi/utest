package test.utest

import utest._

object What {
  def imported = 2
}

object LazyValTest extends TestSuite {
  override def tests = TestSuite {
    import What._
    lazy val subj = imported
    test("test"){
      lazy val y = "hello"
      test("inner"){
        lazy val z = "world"
        val res = y * imported + z
        assert(res == "hellohelloworld")
        res
      }
      test("inner2"){
        lazy val terminalLazyVal = y
      }
    }
  }
}