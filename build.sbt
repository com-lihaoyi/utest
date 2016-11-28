import sbtcross.{crossProject, CrossType}
import org.scalajs.core.tools.sem.CheckedBehavior

crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0")

def macroDependencies(version: String) =
  ("org.scala-lang" % "scala-reflect" % version) +:
  (if (version startsWith "2.10.")
     Seq(compilerPlugin("org.scalamacros" % s"paradise" % "2.1.0" cross CrossVersion.full),
         "org.scalamacros" %% s"quasiquotes" % "2.1.0")
   else
     Seq())

lazy val utest = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(
    libraryDependencies ++= macroDependencies(scalaVersion.value),

    unmanagedSourceDirectories in Compile += {
      val v = if (scalaVersion.value startsWith "2.10.") "scala-2.10" else "scala-2.11"
      baseDirectory.value/".."/"shared"/"src"/"main"/v
    },
//    libraryDependencies += "com.lihaoyi" %% "acyclic" % "0.1.4" % "provided",
//    autoCompilerPlugins := true,
//    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.4"),
    testFrameworks += new TestFramework("test.utest.CustomFramework"),
    scalacOptions := Seq(
      "-Ywarn-dead-code"
    ),
    name := "utest",
    organization := "com.lihaoyi",
    version := "0.4.4",
    scalaVersion := "2.11.8",
    scalacOptions ++= Seq(scalaVersion.value match {
      case x if x.startsWith("2.12.") => "-target:jvm-1.8"
      case x => "-target:jvm-1.6"
    }),
    // Sonatype2
    publishArtifact in Test := false,
    publishTo := Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),

    pomExtra :=
      <url>https://github.com/lihaoyi/utest</url>
      <licenses>
        <license>
          <name>MIT license</name>
          <url>http://www.opensource.org/licenses/mit-license.php</url>
        </license>
      </licenses>
      <scm>
        <url>git://github.com/lihaoyi/utest.git</url>
        <connection>scm:git://github.com/lihaoyi/utest.git</connection>
      </scm>
      <developers>
        <developer>
          <id>lihaoyi</id>
          <name>Li Haoyi</name>
          <url>https://github.com/lihaoyi</url>
        </developer>
      </developers>
  )
  .platformsSettings(JVMPlatform, NativePlatform)(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion,
    scalaJSStage in Test := FastOptStage,
    scalaJSSemantics in Test ~= (_.withAsInstanceOfs(CheckedBehavior.Compliant))
  )
  .jvmSettings(
    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
  .nativeSettings(
    libraryDependencies ++= Seq("org.scala-sbt" % "test-interface" % "1.0"
    )
  )

lazy val utestJS = utest.js
lazy val utestJVM = utest.jvm
lazy val utestNative = utest.native


lazy val sandbox =
  project
    .in(file("sandbox"))
    .settings(scalaVersion := "2.11.8")
    .dependsOn(utestNative % Test)
    .enablePlugins(ScalaNativePlugin)
    .settings(testFrameworks += new TestFramework("utest.runner.Framework"))
