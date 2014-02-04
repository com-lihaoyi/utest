package utest.runner
import sbt.testing.SubclassFingerprint

abstract class GenericTestFramework extends sbt.testing.Framework{

  def name(): String = "utest"

  def fingerprints(): Array[sbt.testing.Fingerprint] = Array(
    new SubclassFingerprint {
      def superclassName = "utest.framework.TestSuite"
      def isModule = true
      def requireNoArgConstructor = true
    }
  )

}
