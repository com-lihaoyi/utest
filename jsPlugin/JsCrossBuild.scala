package utest.jsrunner

import sbt._
import sbt.Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import sbt.TestFramework
import scala.scalajs.sbtplugin.testing.JSClasspathLoader

class JsCrossBuild(sharedSettings: Def.Setting[_]*) extends SelfCrossBuild(
  libraryDependencies += "com.lihaoyi" %% "utest" % "0.1.6-RC1" % "test",
  libraryDependencies += "com.lihaoyi" %%% "utest" % "0.1.6-RC1" % "test",
  sharedSettings
)

/**
 *
 */
class SelfCrossBuild(sharedSettings: Seq[Def.Setting[_]] = Nil,
                     jvmSettings: Seq[Def.Setting[_]] = Nil,
                     jsSettings: Seq[Def.Setting[_]] = Nil){
  val defaultSettings = Seq(
    unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "shared" / "main" / "scala"),
    unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".." / "shared" / "test" / "scala")
  )
  lazy val js = project.in(file("js"))
    .settings(jsSettings ++ sharedSettings ++ scalaJSSettings ++ defaultSettings: _*)
    .settings(
      (loadedTestFrameworks in Test) := {
        (loadedTestFrameworks in Test).value.updated(
          sbt.TestFramework(classOf[JsFramework].getName),
          new JsFramework(environment = (jsEnv in Test).value)
        )
      },
      testLoader := JSClasspathLoader((execClasspath in Compile).value)
    )
  lazy val jvm = project.in(file("jvm"))
    .settings(jvmSettings ++ sharedSettings ++ defaultSettings:_*)
    .settings(
      testFrameworks += new TestFramework("utest.runner.JvmFramework")
    )

  lazy val root = project.in(file("."))
    .aggregate(js, jvm)
    .settings(
      publish := ()
    )
}
