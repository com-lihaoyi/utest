addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.3-SNAPSHOT")

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "js-plugin")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.3",
  "org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3",
  "org.scala-sbt" % "test-interface" % "1.0"
)

addCompilerPlugin("org.scalamacros" % "paradise_2.10.3" % "2.0.0-M3")
