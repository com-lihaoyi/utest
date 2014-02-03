import sbt.Keys._

import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._

lazy val root = project.in(file(".")).dependsOn(file("../js") % "test->test")

scalaJSSettings

addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.3-SNAPSHOT")

//unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".." / "shared" / "test" / "scala")

(loadedTestFrameworks in Test) := {
  println("inConfig works")
  println("A")
  (loadedTestFrameworks in Test).value.updated(
    sbt.TestFramework(classOf[UTestFramework].getName),
    new UTestFramework(environment = (scalaJSEnvironment in Test).value)
  )
}