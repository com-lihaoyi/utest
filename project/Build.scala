import sbt._
import Keys._
import scala.scalajs.sbtplugin.env.nodejs.NodeJSEnv
import scala.scalajs.sbtplugin.env.phantomjs.PhantomJSEnv
import scala.scalajs.sbtplugin.env.rhino.RhinoJSEnv
import scala.scalajs.sbtplugin.ScalaJSPlugin._

import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._
import scala.scalajs.sbtplugin.testing.JSClasspathLoader

import utest.jsrunner._

object Build extends sbt.Build{
  lazy val cross = new BootstrapCrossBuild(
    Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "org.scala-sbt" % "test-interface" % "1.0"
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
  lazy val js = cross.js.settings((jsEnv in Test) := new NodeJSEnv())
  lazy val jvm = cross.jvm.dependsOn(runner)

  lazy val runner = project.settings(sharedSettings:_*)
                           .settings(
    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",
    name := "utest-runner",
    crossScalaVersions := Seq("2.11.2", "2.10.4")
  )

  lazy val jsPlugin = project.in(file("jsPlugin"))
                             .dependsOn(runner)
                             .settings(sharedSettings:_*)
                             .settings(
    addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.5.3"),
    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",
    name := "utest-js-plugin",
    sbtPlugin := true
  )

  lazy val sharedSettings = Seq(
    organization := "com.lihaoyi",
    version := "0.1.9",
    // Sonatype2
    publishArtifact in Test := false,
    publishTo <<= version { (v: String) =>
      Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
    },

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
