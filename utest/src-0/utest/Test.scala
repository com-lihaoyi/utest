package test.utest
import utest._
import utest.framework.TestPath
import scala.util.{Failure, Success}
import utest.framework.Result

import scala.concurrent.ExecutionContext
import scala.util.Success
import scala.util.Failure
import utest.framework.ExecutionContext.RunNow


object FrameworkTests extends utest.TestSuite{
  def tests = Tests{
    test("foo"){
      implicitly[utest.framework.TestPath]
    }
  }
}
