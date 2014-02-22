import sbt.Keys._

libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0"

organization := "com.lihaoyi.utest"

name := "utest-runner"

version := "0.1.1"

Build.sharedSettings
