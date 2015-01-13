package utest
package jsrunner
import sbt._
import sbt.Keys._
import utest.jsrunner._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin._
object Plugin extends sbt.Plugin{
  val utestVersion = "0.2.5-RC1"
  object internal {
    val utestSettings = Seq(
      testFrameworks += new TestFramework("utest.runner.Framework")
    )
    val utestJvmSettings = utestSettings
    val utestJsSettings = utestSettings
  }
  val utestJvmSettings = internal.utestJvmSettings :+ {
    libraryDependencies += "com.lihaoyi" %% "utest" % utestVersion % "test"
  }
  val utestJsSettings = internal.utestJsSettings :+ {
    libraryDependencies += "com.lihaoyi" %%%! "utest" % utestVersion % "test"
  }

}
