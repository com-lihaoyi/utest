package utest

/**
* Test suite for all the assertions that uTest comes bundled with.
*
* I use Predef.assert and manual try-catch-asserts throughout this suite,
* since it is the thing that is meant to be *testing* all the fancy uTest
* asserts, we can't assume they work.
*/
object ConfigurePrintTests extends TestSuite{
  override def formatSingle(path: Seq[String], res: utest.framework.Result) = None
  def tests = this{
    'hello{
      123
    }

  }
}

