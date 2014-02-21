resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.3")

libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0"

name := "utest-js-plugin"

version := "0.1.1"

sbtPlugin := true

Build.sharedSettings
