import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._

import scala.scalajs.sbtplugin.testing.TestFramework

scalaJSSettings

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".."/ "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".." / "shared" / "test" / "scala")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-sbt" % "test-interface" % "1.0",
  "com.lihaoyi.acyclic" %% "acyclic" % "0.1.1" % "provided"
)

libraryDependencies ++= (scalaVersion.value match {
  case v if v startsWith "2.10." => List("org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3")
  case _                         => Nil
})

libraryDependencies += compilerPlugin("org.scalamacros" % s"paradise_${scalaVersion.value}" % "2.0.0-M3")

(loadedTestFrameworks in Test) := {
  (loadedTestFrameworks in Test).value.updated(
    sbt.TestFramework(classOf[utest.runner.JsFramework].getName),
    new utest.runner.JsFramework(environment = (scalaJSEnvironment in Test).value)
  )
}

Build.sharedSettings

autoCompilerPlugins := true

addCompilerPlugin("com.lihaoyi.acyclic" %% "acyclic" % "0.1.1")

name := "utest"

version := "0.1.1-JS"
