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
    test("hello"){
      flaky.run()
    }
  }
}

object SuiteManualRetryTests extends utest.TestSuite{
  override def utestWrap(path: Seq[String], body: => Future[Any])(implicit ec: ExecutionContext): Future[Any] = {
    def rec(count: Int): Future[Any] = {
      utestBeforeEach(path)
      body.recoverWith { case e =>
        if (count < 5) rec(count + 1)
        else throw e
      }.andThen {
        case _ => utestAfterEach(path)
      }
    }
    val res = rec(0)
    res
  }
  val flaky = new FlakyThing
  def tests = Tests{
    test("hello"){
      flaky.run()
    }
  }
}

object SuiteRetryBeforeEachTests extends TestSuite with TestSuite.Retries {
  private var x = 0
  override val utestRetryCount = 3
  override def utestBeforeEach(path: Seq[String]): Unit = {
    x = 0
  }
  val flaky = new FlakyThing
  def tests = Tests{
    test("hello"){
      assert(x == 0)
      x += 1
      flaky.run()
    }
  }
}

object SuiteRetryBeforeAllTests extends TestSuite with TestSuite.Retries {
  override val utestRetryCount = 3
  var x = 100
  val flaky = new FlakyThing
  println(s"starting with x: $x")

  override def utestAfterAll(): Unit = {
    println(s"on after all with x: $x")
    assert(x == 50)
  }

  def tests = Tests {
    test("hello"){
      flaky.run()
      x += 1
      x
    }
    test("test2"){
      assert(x == 101)
      x = 50
      x
    }
  }
}

object LocalRetryTests extends utest.TestSuite{
  val flaky = new FlakyThing
  def tests = Tests{
    test("hello") - retry(3){
      flaky.run()
    }
  }
}

object SuiteRetryBeforeEachFailedTests extends TestSuite with TestSuite.Retries {
  override val utestRetryCount = 3
  override def utestBeforeEach(path: Seq[String]): Unit = {
    flaky.run()
  }
  val flaky = new FlakyThing
  def tests = Tests{
    test("hello"){
      1
    }
  }
}

object SuiteRetryAfterEachFailedTests extends TestSuite with TestSuite.Retries {
  val flaky = new FlakyThing
  override val utestRetryCount = 1
  override def utestAfterEach(path: Seq[String]): Unit = {
    flaky.run()
  }
  def tests = Tests {
    test("hello"){
      1
    }
  }
}
