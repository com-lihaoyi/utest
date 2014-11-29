package utest.runner
import sbt.testing.SubclassFingerprint

final class Framework extends sbt.testing.Framework{

  def name(): String = "utest"

  def fingerprints(): Array[sbt.testing.Fingerprint] = Array(
    new SubclassFingerprint {
      def superclassName = "utest.framework.TestSuite"
      def isModule = true
      def requireNoArgConstructor = true
    }
  )

  def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader) = {
    new MasterRunner(args, remoteArgs, testClassLoader)
  }

  def slaveRunner(args: Array[String],
                  remoteArgs: Array[String],
                  testClassLoader: ClassLoader,
                  send: String => Unit) = {
    new SlaveRunner(args, remoteArgs, testClassLoader, send)
  }
}
