package utest.runner
import sbt.testing.SubclassFingerprint
import scala.scalajs.sbtplugin.ScalaJSEnvironment


class JsTestFramework(environment: ScalaJSEnvironment) extends utest.runner.GenericTestFramework{

  def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader) = {
    new JsRunner(args, remoteArgs, environment)
  }
}
