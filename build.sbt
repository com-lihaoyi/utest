lazy val js = project.in(file("js"))

lazy val runner = project.in(file("runner"))

lazy val root = project.in(file(".")).dependsOn(runner)

lazy val jsPlugin = project.in(file("js-plugin")).dependsOn(runner)

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / "shared" / "test" / "scala")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

Build.sharedSettings

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.3",
  "org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3",
  "org.scala-sbt" % "test-interface" % "1.0",
  "com.lihaoyi.acyclic" %% "acyclic" % "0.1.0" % "provided"
)

addCompilerPlugin("org.scalamacros" % "paradise_2.10.3" % "2.0.0-M3")

autoCompilerPlugins := true

addCompilerPlugin("com.lihaoyi.acyclic" %% "acyclic" % "0.1.0")

testFrameworks += new TestFramework("utest.runner.JvmFramework")

version := "0.1.1"

name := "utest"
