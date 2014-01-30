import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._

import scala.scalajs.sbtplugin.testing.TestFramework

addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.3-SNAPSHOT")

scalaJSSettings

//unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "shared" / "main" / "scala")

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "shared" / "test" / "scala")