import sbt._
import Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin.scalaJSSettings
import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys.scalaJSEnvironment

/**
 * Bootstrapping build, used for the initial publishLocal. Leaves out test
 * dependencies because it hasn't been published to be depended on yet
 */
//object BootBuild extends Build(Nil, Nil)

/**
 * Test-specific requirements; comment out during the first +publishLocal
 */
object TestBuild extends Build(
  Seq(
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.1.3-JS",
    (loadedTestFrameworks in Test) := {
      (loadedTestFrameworks in Test).value.updated(
        sbt.TestFramework(classOf[utest.runner.JsFramework].getName),
        new utest.runner.JsFramework(environment = (scalaJSEnvironment in Test).value)
      )
    },
    name := "utest-test-js"
  ),
  Seq(
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.1.3",
    testFrameworks += new TestFramework("utest.runner.JvmFramework"),
    name := "utest-test"
  )
)

class Build(jsSettings: Seq[Def.Setting[_]], jvmSettings: Seq[Def.Setting[_]]) extends sbt.Build{

  val crossSetting = crossScalaVersions := Seq("2.10.4", "2.11.0")

  lazy val js = project.in(file("js"))
                       .settings(sharedSettings ++ libSettings ++ scalaJSSettings ++ jsSettings:_*)
                       .settings(version := version.value + "-JS")

  lazy val runner = project.in(file("runner"))
                           .settings(sharedSettings:_*)
                           .settings(
    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",
    name := "utest-runner"
  )

  lazy val root = project.in(file("."))
                         .settings(crossScalaVersions := Seq("2.10.4", "2.11.0"))
                         .aggregate(js, jvm)

  lazy val plugins = project.aggregate(runner, jsPlugin)

  lazy val jvm = project.in(file("jvm"))
                         .dependsOn(runner)
                         .settings(sharedSettings ++ libSettings ++ jvmSettings:_*)


  lazy val jsPlugin = project.in(file("js-plugin"))
                             .dependsOn(runner)
                             .settings(sharedSettings:_*)
                             .settings(
    addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.4.3"),
    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",
    name := "utest-js-plugin",
    sbtPlugin := true
  )


  val libSettings = Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-sbt" % "test-interface" % "1.0",
      compilerPlugin("org.scalamacros" % s"paradise" % "2.0.0" cross CrossVersion.full)
    ) ++ (
      if (scalaVersion.value startsWith "2.11.") Nil
      else Seq(
        "org.scalamacros" %% s"quasiquotes" % "2.0.0"
      )
    ),
    crossSetting,
    name := "utest",
    unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "shared" / "main" / "scala"),
    unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".." / "shared" / "test" / "scala")
  )

  val sharedSettings = Seq(
    organization := "com.lihaoyi",
    scalaVersion := "2.10.4",
    version := "0.1.3",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
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
