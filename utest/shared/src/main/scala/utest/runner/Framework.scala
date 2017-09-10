package utest
package runner
//import acyclic.file
import sbt.testing.SubclassFingerprint
import sbt.testing.EventHandler

class Framework extends sbt.testing.Framework{

  def name(): String = "utest"

  def setup() = ()
  def teardown() = ()
  def showSummaryThreshold = 20

  def resultsHeader = BaseRunner.renderBanner("Results")
  def failureHeader = BaseRunner.renderBanner("Failures")

  def fingerprints(): Array[sbt.testing.Fingerprint] = Array(
    new SubclassFingerprint {
      def superclassName = "utest.TestSuite"
      def isModule = true
      def requireNoArgConstructor = true
    }
  )

  def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader) = {
    new MasterRunner(
      args,
      remoteArgs,
      testClassLoader,
      setup,
      teardown,
      showSummaryThreshold,
      resultsHeader,
      failureHeader
    )
  }

  def slaveRunner(args: Array[String],
                  remoteArgs: Array[String],
                  testClassLoader: ClassLoader,
                  send: String => Unit) = {
    new ScalaJsSlaveRunner(args, remoteArgs, testClassLoader, send, setup, teardown)
  }
}
