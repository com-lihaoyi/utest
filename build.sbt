lazy val js = project.in(file("js"))

lazy val runner = project.in(file("runner"))

lazy val root = project.in(file(".")).dependsOn(runner)

lazy val jsPlugin = project.in(file("js-plugin")).dependsOn(runner)

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / "shared" / "test" / "scala")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.3",
  "org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3",
  "org.scala-sbt" % "test-interface" % "1.0"
)

addCompilerPlugin("org.scalamacros" % "paradise_2.10.3" % "2.0.0-M3")

testFrameworks += new TestFramework("utest.runner.JvmFramework")

organization := "com.lihaoyi"

name := "utest"

scalaVersion := "2.10.3"

version := "0.1.0"