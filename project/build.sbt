def versionSettings(sv: String) = (
  if (sv startsWith "2.10.") Seq(
    Defaults.sbtPluginExtra("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.4-SNAPSHOT", "0.13", "2.10"),
    compilerPlugin("org.scalamacros" % "paradise_2.10.3" % "2.0.0-M3"),
    "org.scala-lang" % "scala-reflect" % "2.10.3",
    "org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3",
    "org.scala-sbt" % "test-interface" % "1.0"
  )
  else Seq(
    Defaults.sbtPluginExtra("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.4-SNAPSHOT", "0.13", "2.11.0-M8"),
    compilerPlugin("org.scalamacros" % "paradise_2.11.0-M8" % "2.0.0-M3"),
    "org.scala-lang" % "scala-reflect" % "2.11.0-M8",
    "org.scala-sbt" % "test-interface" % "1.0"
  )
)

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "js-plugin")

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "runner")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= versionSettings(scalaVersion.value)
