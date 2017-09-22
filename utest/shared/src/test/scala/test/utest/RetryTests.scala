package test.utest
import utest._
import scala.concurrent.{ExecutionContext, Future}

class FlakyThing{
  var runs = 0
  def run() = {
    runs += 1
    if (runs < 2) throw new Exception("Flaky!")
  }
}
object SuiteRetryTests extends TestSuite with TestSuite.Retries{
  override val utestRetryCount = 3
  val flaky = new FlakyThing
  def tests = Tests{
    'hello - {
      flaky.run
    }
  }
}

object SuiteManualRetryTests extends utest.TestSuite{
  override def utestWrap(path: Seq[String], body: => Future[Any])(implicit ec: ExecutionContext): Future[Any] = {
    def rec(count: Int): Future[Any] = {
      utestBeforeEach()
      body.recoverWith { case e =>
        if (count < 5) rec(count + 1)
        else throw e
      }.andThen {
        case _ => utestAfterEach()
      }
    }
    val res = rec(0)
    res
  }
  val flaky = new FlakyThing
  def tests = Tests{
    'hello - {
      flaky.run
    }
  }
}

object SuiteRetryBeforeEachTests extends TestSuite with TestSuite.Retries {
  var x = 0
  override val utestRetryCount = 3
  override def utestBeforeEach() = {
    x = 0
  }
  val flaky = new FlakyThing
  def tests = Tests{
    'hello - {
      assert(x == 0)
      x += 1
      flaky.run
    }
  }
}

object LocalRetryTests extends utest.TestSuite{
  val flaky = new FlakyThing
  def tests = Tests{
    'hello - retry(3){
      flaky.run
    }
  }
}

// TODO: bug with retry assert
// object LocalRetryBeforeEachTests extends utest.TestSuite{
//   var x = 0
//   override def utestBeforeEach() = {
//     println(s">>> before each x: $x")
//     x = 0
//   }
//   val flaky = new FlakyThing

//   def tests = Tests{
//     'hello - retry(3){
//       println(s">>> x: $x")
//       assert(x == 0)
//       x += 1
//       flaky.run
//     }
//   }
// }

// -------------------------------- Running Tests --------------------------------
//   Setting up CustomFramework
//   >>> before each x: 0
//   >>> x: 0
//   >>> x: 1
//   >>> x: 1
//   >>> x: 1
// X test.utest.LocalRetryBeforeEachTests.hello 20ms
// utest.AssertionError: assert(x == 0)
// utest.asserts.Asserts$.assertImpl(Asserts.scala:114)
