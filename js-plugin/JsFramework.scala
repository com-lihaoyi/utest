package utest.jsrunner
import sbt.testing.SubclassFingerprint
import scala.scalajs.sbtplugin.ScalaJSEnvironment

import utest.runner._

class JsFramework(environment: ScalaJSEnvironment) extends utest.runner.GenericTestFramework{
  def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader) = {
    new JsRunner(args, remoteArgs, environment)
  }
}
