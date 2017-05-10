import com.typesafe.sbt.pgp.PgpKeys._

name                          in ThisBuild := "utest"
organization                  in ThisBuild := "com.lihaoyi"
scalaVersion                  in ThisBuild := "2.12.2"
crossScalaVersions            in ThisBuild := Seq("2.10.6", "2.11.11", "2.12.2", "2.13.0-M1")
updateOptions                 in ThisBuild := (updateOptions in ThisBuild).value.withCachedResolution(true)
incOptions                    in ThisBuild := (incOptions in ThisBuild).value.withNameHashing(true).withLogRecompileOnMacro(false)
triggeredMessage              in ThisBuild := Watched.clearWhenTriggered
releasePublishArtifactsAction in ThisBuild := PgpKeys.publishSigned.value
releaseTagComment             in ThisBuild := s"v${(version in ThisBuild).value}"
releaseVcsSign                in ThisBuild := true

lazy val utest = crossProject
  .settings(
    scalacOptions         := Seq("-Ywarn-dead-code"),
    scalacOptions in Test -= "-Ywarn-dead-code",
    libraryDependencies  ++= macroDependencies(scalaVersion.value),
    scalacOptions        ++= (scalaVersion.value match {
      case x if x startsWith "2.13." => "-target:jvm-1.8" :: Nil
      case x if x startsWith "2.12." => "-target:jvm-1.8" :: "-opt:l:method" :: Nil
      case x if x startsWith "2.11." => "-target:jvm-1.6" :: Nil
      case x if x startsWith "2.10." => "-target:jvm-1.6" :: Nil
    }),

    unmanagedSourceDirectories in Compile += {
      val v = if (scalaVersion.value startsWith "2.10.") "scala-2.10" else "scala-2.11"
      baseDirectory.value/".."/"shared"/"src"/"main"/v
    },
    testFrameworks += new TestFramework("test.utest.CustomFramework"),

    // Sonatype2
    publishArtifact in Test := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    homepage := Some(url("https://github.com/lihaoyi/utest")),
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
    )
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "test-interface" % "1.0",
      "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
    ),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )

def macroDependencies(version: String) =
  ("org.scala-lang" % "scala-reflect" % version) +:
  (if (version startsWith "2.10.")
     Seq(compilerPlugin("org.scalamacros" % s"paradise" % "2.1.0" cross CrossVersion.full),
         "org.scalamacros" %% s"quasiquotes" % "2.1.0")
   else
     Seq())

lazy val utestJS = utest.js
lazy val utestJVM = utest.jvm

lazy val root = project.in(file("."))
  .aggregate(utestJS, utestJVM)
  .settings(
    publishTo := Some(Resolver.file("Unused transient repository", target.value / "fakepublish")),
    publishArtifact := false,
    publishLocal := (),
    publishLocalSigned := (),       // doesn't work
    publishSigned := (),            // doesn't work
    packagedArtifacts := Map.empty) // doesn't work - https://github.com/sbt/sbt-pgp/issues/42

