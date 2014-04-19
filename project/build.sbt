resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= (if (scalaVersion.value startsWith "2.10.") Seq(
  Defaults.sbtPluginExtra("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.4.3", "0.13", "2.10")
) else Seq(
  Defaults.sbtPluginExtra("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.4.3", "0.13", "2.11.0")
))
