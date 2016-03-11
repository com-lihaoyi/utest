package utest.runner
import acyclic.file
import sbt.testing.SubclassFingerprint
import sbt.testing.EventHandler

class Framework extends sbt.testing.Framework{

  def name(): String = "utest"

  def setup() = ()
  def teardown() = ()
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
    new MasterRunner(args, remoteArgs, testClassLoader, setup, teardown)
  }

  def slaveRunner(args: Array[String],
                  remoteArgs: Array[String],
                  testClassLoader: ClassLoader,
                  send: String => Unit) = {
    new ScalaJsSlaveRunner(args, remoteArgs, testClassLoader, send, setup, teardown)
  }
}
