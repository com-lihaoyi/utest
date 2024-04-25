package test.utest

import sbt.testing._
import utest._
import utest.runner.{Fingerprint, Framework}

import scala.collection.mutable.ArrayBuffer

object SelectorTest extends utest.TestSuite {
  private val helperFqcn = HelperTest.getClass.getName.stripSuffix("$")

  def tests = Tests {
    test("SuiteSelector runs entire suite") {
      val events = runWithSelectors(new SuiteSelector :: Nil)
      val expectedNames = Set(s"$helperFqcn.simple", s"$helperFqcn.successful test", s"$helperFqcn.with a nested test.this is nested")
      assert(events.map(testName).toSet == expectedNames)
    }

    test("NestedSuiteSelector runs only the matching test") {
      val events = runWithSelectors(new NestedTestSelector(helperFqcn, "simple") :: Nil)
      assert(events.map(testName) == List(s"$helperFqcn.simple"))
    }

    test("NestedSuiteSelector works with spaces") {
      val events = runWithSelectors(new NestedTestSelector(helperFqcn, "successful test") :: Nil)
      assert(events.map(testName) == List(s"$helperFqcn.successful test"))
    }

    test("NestedSuiteSelector works with nested tests") {
      val events = runWithSelectors(new NestedTestSelector(helperFqcn, "with a nested test") :: Nil)
      val expected = List(s"$helperFqcn.with a nested test.this is nested")
      assert(events.map(testName) == expected)

      val events2 = runWithSelectors(new NestedTestSelector(helperFqcn, "with a nested test.this is nested") :: Nil)
      assert(events2.map(testName) == expected)
    }
  }

  private def testName(event: Event): String = event.selector() match {
    case nts: NestedTestSelector => s"${nts.suiteId()}.${nts.testName()}"
  }

  private def runWithSelectors(selectors: List[Selector]): List[Event] = {
    val loggers = Array(NoLogger: Logger)
    val events = ArrayBuffer.empty[Event]
    val handler: EventHandler = (e: Event) => events.append(e)
    val framework = new Framework()
    val runner = framework.runner(Array.empty, Array.empty, getClass.getClassLoader)

    val taskDef = new TaskDef(helperFqcn, Fingerprint, true, selectors.toArray)
    val tasks = runner.tasks(Array(taskDef))
    tasks.foreach(_.execute(handler, loggers))

    events.toList
  }

}

private object HelperTest extends utest.TestSuite {
  def tests = Tests {
    test("simple") { () }
    test("successful test") { () }
    test("with a nested test") {
      test("this is nested") { () }
    }
  }
}

private object NoLogger extends Logger {
  override def ansiCodesSupported(): Boolean = false
  override def error(msg: String): Unit = ()
  override def warn(msg: String): Unit = ()
  override def info(msg: String): Unit = ()
  override def debug(msg: String): Unit = ()
  override def trace(t: Throwable): Unit = ()
}
