import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import org.scalajs.core.tools.sem.CheckedBehavior

import utest.jsrunner._

object Build extends sbt.Build{
  lazy val cross = new BootstrapCrossBuild(
    Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      ) ++ (
        if (scalaVersion.value startsWith "2.11.") Nil
        else Seq(
          compilerPlugin("org.scalamacros" % s"paradise" % "2.0.0" cross CrossVersion.full),
          "org.scalamacros" %% s"quasiquotes" % "2.0.0"
        )
      ),
      name := "utest"
    ) ++ sharedSettings
  )

  lazy val root = cross.root
  lazy val js = cross.js.settings(
    libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion,
    scalaJSStage in Test := FastOptStage,
    scalaJSSemantics in Test ~= (_.withAsInstanceOfs(CheckedBehavior.Compliant)),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
  lazy val jvm = cross.jvm.settings(
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "test-interface" % "1.0",
      "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided",
      "com.typesafe.akka" %% "akka-actor" % "2.3.2" % "test"
    ),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )

  lazy val jsPlugin = project.in(file("jsPlugin"))
                             .settings(sharedSettings:_*)
                             .settings(

    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.0-M3"),
    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",
    name := "utest-js-plugin",
    sbtPlugin := true
  )

  lazy val sharedSettings = Seq(
    organization := "com.lihaoyi",
    version := Plugin.utestVersion,
    scalaVersion := "2.10.4",
    // Sonatype2
    publishArtifact in Test := false,
    publishTo := Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),

    pomExtra := (
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
  )
}
