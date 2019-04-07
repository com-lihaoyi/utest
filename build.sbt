import sbtcrossproject.{crossProject, CrossType}
import com.typesafe.sbt.pgp.PgpKeys._
import sbt.Keys.scalacOptions
import sbt.addCompilerPlugin
import sbt.librarymanagement.{SemanticSelector, VersionNumber}

name               in ThisBuild := "utest"
organization       in ThisBuild := "com.lihaoyi"
scalaVersion       in ThisBuild := "2.12.8"
crossScalaVersions in ThisBuild := Seq("2.10.7", "2.11.12", "2.12.8", "2.13.0-RC1")
updateOptions      in ThisBuild := (updateOptions in ThisBuild).value.withCachedResolution(true)
incOptions         in ThisBuild := (incOptions in ThisBuild).value.withLogRecompileOnMacro(false)
//triggeredMessage   in ThisBuild := Watched.clearWhenTriggered

lazy val utest = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(
    name                  := "utest",
    scalacOptions         := Seq("-Ywarn-dead-code", "-feature"),
    scalacOptions in Test -= "-Ywarn-dead-code",
    libraryDependencies  ++= macroDependencies(scalaVersion.value),
    scalacOptions        ++= (scalaVersion.value match {
      case x if x startsWith "2.13." => "-target:jvm-1.8" :: Nil
      case x if x startsWith "2.12." => "-target:jvm-1.8" :: "-opt:l:method" :: Nil
      case x if x startsWith "2.11." => "-target:jvm-1.6" :: Nil
      case x if x startsWith "2.10." => "-target:jvm-1.6" :: Nil
    }),

    unmanagedSourceDirectories in Compile += {
      val v = "scala-" + scalaVersion.value.split("\\.").take(2).mkString(".")
      baseDirectory.value/".."/"shared"/"src"/"main"/v
    },
    unmanagedSourceDirectories in Compile ++= {
      if (VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("<2.13.0-RC1"))) {
        baseDirectory.value/".."/"shared"/"src"/"main"/"scala-pre-2.13" :: Nil
      } else {
        Nil
      }
    },
    testFrameworks += new TestFramework("test.utest.CustomFramework"),

    // Release settings
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
    )//,
//    autoCompilerPlugins := true,
//
//    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.7"),
//
//    scalacOptions += "-P:acyclic:force"

)
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion,
      "org.portable-scala" %%% "portable-scala-reflect" % "0.1.0"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "test-interface" % "1.0",
      "org.portable-scala" %%% "portable-scala-reflect" % "0.1.0"
    ),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
  .nativeSettings(
    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12"),
    libraryDependencies ++= Seq(
      "org.scala-native" %%% "test-interface" % "0.3.0"
    ),
    nativeLinkStubs := true
  )

def macroDependencies(version: String) =
  ("org.scala-lang" % "scala-reflect" % version) +:
  (if (version startsWith "2.10.")
     Seq(compilerPlugin("org.scalamacros" % s"paradise" % "2.1.0" cross CrossVersion.full),
         "org.scalamacros" %% s"quasiquotes" % "2.1.0")
   else
     Seq())

lazy val root = project.in(file("."))
  .aggregate(utest.js, utest.jvm, utest.native)
  .settings(
    publishTo := Some(Resolver.file("Unused transient repository", target.value / "fakepublish")),
    skip in publish := true)

// Settings for release from Travis on tag push
inScope(Global)(
  Seq(
    credentials ++= (for {
      username <- sys.env.get("SONATYPE_USERNAME")
      password <- sys.env.get("SONATYPE_PASSWORD")
    } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toList,
    PgpKeys.pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray())
  )
)
