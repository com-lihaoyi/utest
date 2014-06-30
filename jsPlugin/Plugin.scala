package utest
package jsrunner
import sbt._
import sbt.Keys._
import utest.jsrunner._
import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
object Plugin extends sbt.Plugin{
  object internal {
    val utestJvmSettings = Seq(
      testFrameworks += new TestFramework("utest.runner.JvmFramework")
    )
    val utestJsSettings = {
      val utestTestFrameworkSettings = Seq(
        loadedTestFrameworks +=
          sbt.TestFramework(classOf[JsFramework].getName) ->
            new JsFramework(environment = jsEnv.value)
      )
      val utestTestSettings =
        utestTestFrameworkSettings ++
          inTask(packageStage)(utestTestFrameworkSettings) ++
          inTask(fastOptStage)(utestTestFrameworkSettings) ++
          inTask(fullOptStage)(utestTestFrameworkSettings)
      inConfig(Test)(utestTestSettings)
    }
  }
  val utestJvmSettings = internal.utestJvmSettings :+ {
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.1.7" % "test"
  }
  val utestJsSettings = internal.utestJsSettings :+ {
    libraryDependencies += "com.lihaoyi" %%% "utest" % "0.1.7" % "test"
  }
}
