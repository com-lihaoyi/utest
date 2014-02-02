import sbt.testing.SubclassFingerprint
import scala.scalajs.sbtplugin.ScalaJSEnvironment


class UTestFramework(environment: ScalaJSEnvironment) extends sbt.testing.Framework{
  println("UTestFramework " + environment)
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
             testClassLoader: ClassLoader): Runner = {
    new Runner(args, remoteArgs, environment)
  }
}
