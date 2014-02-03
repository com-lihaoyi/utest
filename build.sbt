lazy val root = project.in(file("."))

lazy val js = project.in(file("js"))

lazy val jsPlugin = project.in(file("js-plugin"))

lazy val jsTest = project.in(file("js-test"))

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

testFrameworks += new TestFramework("utest.runner.Framework")
