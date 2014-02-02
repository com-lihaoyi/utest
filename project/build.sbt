addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.3-SNAPSHOT")

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "js-plugin")