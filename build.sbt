import org.scalajs.core.tools.sem.CheckedBehavior

val baseSettings = Seq(
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0-RC2"),
  name := "utest",
  organization := "com.lihaoyi",
  version := "0.4.4",
  scmInfo := Some(ScmInfo(
    browseUrl = url("https://github.com/lihaoyi/utest"),
    connection = "scm:git:git@github.com:lihaoyi/utest.git"
  )),
  licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/mit-license.html")),
  developers += Developer(
    email = "haoyi.sg@gmail.com",
    id = "lihaoyi",
    name = "Li Haoyi",
    url = url("https://github.com/lihaoyi")
  ),
  // Sonatype2
  publishArtifact in Test := false,
  publishTo := Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
)

baseSettings

def macroDependencies(version: String, binaryVersion: String) = {
  val quasiquotes = 
    if(binaryVersion == "2.10")
      Seq(
        compilerPlugin("org.scalamacros" % s"paradise" % "2.1.0" cross CrossVersion.full),
        "org.scalamacros" %% s"quasiquotes" % "2.1.0"
      )
    else Seq()
  ("org.scala-lang" % "scala-reflect" % version) +: quasiquotes
}

def akkaVersionFrom(scalaVersion: String): String = scalaVersion match {
  case x if x.startsWith("2.10.") => "2.3.2" //scala 2.10 support
  case _ => "2.4.11" //scala 2.11,2.12 support
}

lazy val utest = crossProject
  .settings(
    libraryDependencies ++= macroDependencies(scalaVersion.value, scalaBinaryVersion.value),
    unmanagedSourceDirectories in Compile += {
      val v = if (scalaBinaryVersion.value == "2.10") "scala-2.10" else "scala-2.11"
      baseDirectory.value/".."/"shared"/"src"/"main"/v
    },
//    libraryDependencies += "com.lihaoyi" %% "acyclic" % "0.1.4" % "provided",
//    autoCompilerPlugins := true,
//    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.4"),
    testFrameworks += new TestFramework("test.utest.CustomFramework"),
    scalacOptions := Seq("-Ywarn-dead-code"),
    scalacOptions ++= Seq(scalaVersion.value match {
      case x if x.startsWith("2.12.") => "-target:jvm-1.8"
      case x => "-target:jvm-1.6"
    })
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion,
    scalaJSStage in Test := FastOptStage,
    scalaJSSemantics in Test ~= (_.withAsInstanceOfs(CheckedBehavior.Compliant)),
    scalaJSUseRhino in Global := false
  )
  .jvmSettings(
//    fork in Test := true,
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "test-interface" % "1.0",
      "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided",
      "com.typesafe.akka" %% "akka-actor" % akkaVersionFrom(scalaVersion.value) % "test"
    ),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )

lazy val utestJS = utest.js
lazy val utestJVM = utest.jvm

