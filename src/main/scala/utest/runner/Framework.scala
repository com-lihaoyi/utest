package utest.runner

import sbt.testing.SubclassFingerprint
import utest.DefaultFormatter


class Framework extends sbt.testing.Framework{
  def name(): String = "utest"

  def fingerprints(): Array[sbt.testing.Fingerprint] = Array(
    new SubclassFingerprint {
      def superclassName = "utest.framework.TestSuite"
      def isModule = true
      def requireNoArgConstructor = true
    }
  )

  def printer(args: Array[String]) = DefaultFormatter(args)

  def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader): Runner = {
    new Runner(args, remoteArgs, printer(args))
  }
}
