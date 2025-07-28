package utest.runner

import sbt.testing.SubclassFingerprint


object Fingerprint extends SubclassFingerprint {
  def superclassName() = "utest.TestSuite"
  def isModule() = true
  def requireNoArgConstructor() = true
}

object ClassFingerprint extends SubclassFingerprint {
  def superclassName() = "utest.TestSuite"
  def isModule() = false
  def requireNoArgConstructor() = true
}
