import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._

import scala.scalajs.sbtplugin.testing.TestFramework

scalaJSSettings

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".."/ "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".." / "shared" / "test" / "scala")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.3",
  "org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3",
  "org.scala-sbt" % "test-interface" % "1.0"
)

addCompilerPlugin("org.scalamacros" % "paradise_2.10.3" % "2.0.0-M3")

(loadedTestFrameworks in Test) := {
  (loadedTestFrameworks in Test).value.updated(
    sbt.TestFramework(classOf[utest.runner.JsFramework].getName),
    new utest.runner.JsFramework(environment = (scalaJSEnvironment in Test).value)
  )
}

organization := "com.lihaoyi"

name := "utest"

scalaVersion := "2.10.3"

version := "0.1.0-JS"