package utest
package runner
//import acyclic.file
import sbt.testing.SubclassFingerprint
import sbt.testing.EventHandler

class Framework extends sbt.testing.Framework{

  def name(): String = "utest"

  /**
    * Override to run code before tests start running. Useful for setting up
    * global databases, initializing caches, etc.
    */
  def setup() = ()

  /**
    * Override to run code after tests finish running. Useful for shutting
    * down daemon processes, closing network connections, etc.
    */
  def teardown() = ()

  /**
    * How many tests need to run before uTest starts printing out the test
    * results summary and test failure summary at the end of a test run. For
    * small sets of tests, these aren't necessary since all the output fits
    * nicely on one screen; only when the number of tests gets large and their
    * output gets noisy does it become valuable to show the clean summaries at
    * the end of the test run.
    */
  def showSummaryThreshold = 40

  def resultsHeader = BaseRunner.renderBanner("Results")
  def failureHeader = BaseRunner.renderBanner("Failures")


  def startHeader(path: String) = BaseRunner.renderBanner("Running Tests" + path)


  final def fingerprints(): Array[sbt.testing.Fingerprint] = Array(Fingerprint)

  final def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader) = {
    new MasterRunner(
      args,
      remoteArgs,
      testClassLoader,
      setup,
      teardown,
      showSummaryThreshold,
      startHeader,
      resultsHeader,
      failureHeader
    )
  }

  final def slaveRunner(args: Array[String],
                  remoteArgs: Array[String],
                  testClassLoader: ClassLoader,
                  send: String => Unit) = {
    new ScalaJsSlaveRunner(args, remoteArgs, testClassLoader, send, setup, teardown)
  }
}
