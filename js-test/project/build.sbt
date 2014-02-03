addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.3-SNAPSHOT")

lazy val root = project.in(file(".")).dependsOn(file("../js-plugin"))


