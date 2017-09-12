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
      body.recoverWith { case e =>
        if (count < 5) rec(count + 1)
        else throw e
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

object LocalRetryTests extends utest.TestSuite{
  val flaky = new FlakyThing
  def tests = Tests{
    'hello - retry(3){
      flaky.run
    }
  }
}

