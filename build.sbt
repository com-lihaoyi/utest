def versionDependencies(sv: String) = ( // sv: scalaVersion
  ("org.scala-lang" % "scala-reflect" % sv) :: (
    if (sv startsWith "2.10.") List(
      compilerPlugin("org.scalamacros" % s"paradise_$sv" % "2.0.0-M3"),
      "org.scalamacros" % s"quasiquotes_$sv" % "2.0.0-M3",
      compilerPlugin("com.lihaoyi.acyclic" %% s"acyclic" % "0.1.0"),
      "com.lihaoyi.acyclic" %% s"acyclic" % "0.1.0" % "provided"
    )
    else Nil
  )
)

lazy val js, runner = project

lazy val root = project in file(".") dependsOn runner

lazy val jsPlugin = project in file("js-plugin") dependsOn runner

autoCompilerPlugins := true

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / "shared" / "test" / "scala")

resolvers ++= Seq(Opts.resolver.sonatypeSnapshots, Opts.resolver.sonatypeReleases)

Build.sharedSettings

libraryDependencies ++= versionDependencies(scalaVersion.value) :+ ("org.scala-sbt" % "test-interface" % "1.0")

testFrameworks += new TestFramework("utest.runner.JvmFramework")

version := "0.1.1"

name := "utest"

scalaVersion := "2.10.3"
