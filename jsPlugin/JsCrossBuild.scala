package utest.jsrunner

import sbt._
import sbt.Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import sbt.TestFramework
import scala.scalajs.sbtplugin.testing.JSClasspathLoader

/**
 * A standard way of defining cross-js/jvm builds. Defines two sub-projects
 * in the `jvm/` and `js/` folders, and a `shared/` folder which contains
 * any sources shared by both projects.
 * 
 * @param sharedSettings Settings that will get applied to both projects, 
 *                       not strictly necessary but pretty convenient.
 */
class JsCrossBuild(sharedSettings: Def.Setting[_]*) extends BootstrapCrossBuild(
  sharedSettings,
  libraryDependencies += "com.lihaoyi" %% "utest" % "0.1.6" % "test",
  libraryDependencies += "com.lihaoyi" %%% "utest" % "0.1.6" % "test"
)

/**
 * A limited version of [[JsCrossBuild]] that does not add dependencies
 * on the utest artifacts, used to bootstrap utest's own test process.
 */
class BootstrapCrossBuild(sharedSettings: Seq[Def.Setting[_]] = Nil,
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
      publish := (),
      crossScalaVersions := Seq("2.11.1", "2.10.4")
    )
}
