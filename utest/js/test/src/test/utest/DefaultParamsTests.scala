package test.utest

import utest._
import scala.scalajs.js
import js.annotation.JSExport

@js.native
trait WithDefaultParams extends js.Object{
  def cmp(a: Int, b: Int = 5): Boolean = js.native
}

object ImplementationWithDefaultParams {
  @JSExport
  def cmp(a: Int, b: Int = 5): Boolean = a > b
}

object DefaultParamsTests extends TestSuite{
  def obj = ImplementationWithDefaultParams.asInstanceOf[WithDefaultParams]

  val tests = Tests{
    test("usedToCrashScalajsCompiler"){
      utest.assert(obj.cmp(10))
      obj.cmp(10)
    }
  }
}
