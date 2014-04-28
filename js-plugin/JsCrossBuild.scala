package utest.jsrunner

import sbt._
import sbt.Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._
import scala.scalajs.sbtplugin.ScalaJSPlugin.scalaJSSettings
import utest.runner._

class JsCrossBuild(sharedSettings: Def.Setting[_]*){
  val defaultSettings = Seq(
    unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "shared" / "main" / "scala"),
    unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".." / "shared" / "test" / "scala")
  )
  lazy val js = project.in(file("js"))
    .settings(sharedSettings ++ scalaJSSettings ++ defaultSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "utest" % "0.1.4-JS" % "test"
      ),
      (loadedTestFrameworks in Test) := {
        (loadedTestFrameworks in Test).value.updated(
          sbt.TestFramework(classOf[JsFramework].getName),
          new utest.jsrunner.JsFramework(environment = (scalaJSEnvironment in Test).value)
        )
      },
      version := version.value + "-JS"
    )
  lazy val jvm = project.in(file("jvm"))
    .settings(sharedSettings ++ defaultSettings:_*)
    .settings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "utest" % "0.1.4" % "test"
      ),
      testFrameworks += new TestFramework("utest.runner.JvmFramework")
    )

  lazy val root = project.in(file("."))
    .aggregate(js, jvm)
    .settings(

      crossScalaVersions := Seq("2.10.4", "2.11.0"),
      scalaVersion := "2.10.4"
    )
}
