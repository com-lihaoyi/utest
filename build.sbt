import org.scalajs.core.tools.sem.CheckedBehavior

crossScalaVersions := Seq("2.10.4", "2.11.4", "2.12.0-M3")

def macroDependencies(version: String) =
  ("org.scala-lang" % "scala-reflect" % version) +:
  (if (version startsWith "2.10.")
     Seq(compilerPlugin("org.scalamacros" % s"paradise" % "2.0.0" cross CrossVersion.full),
         "org.scalamacros" %% s"quasiquotes" % "2.0.0")
   else
     Seq())

def akkaVersionFrom(scalaVersion: String): String = scalaVersion match {
  case x if x.startsWith("2.10.") => "2.3.2" //scala 2.10 support
  case _ => "2.4.2" //scala 2.11,2.12 support
}

lazy val utest = crossProject
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
    version := "0.3.2-SNAPSHOT",
    scalaVersion := "2.11.4",
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

