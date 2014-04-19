import sbt._
import Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin.scalaJSSettings
object Build extends sbt.Build{
  lazy val js = project.in(file("js"))
                       .settings(sharedSettings ++ libSettings ++ scalaJSSettings:_*)
                       .settings(
    version := version.value + "-JS"
  )

  lazy val runner = project.in(file("runner"))
                           .settings(sharedSettings:_*)
                           .settings(
    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",
    name := "utest-runner"
  )

  lazy val jvm = project.in(file("jvm"))
                         .dependsOn(runner)
                         .settings(sharedSettings ++ libSettings:_*)
                         .aggregate(runner, jsPlugin, js)

  lazy val jsPlugin = project.in(file("js-plugin"))
                             .dependsOn(runner)
                             .settings(sharedSettings:_*)
                             .settings(
    addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.3"),
    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",
    name := "utest-js-plugin",
    sbtPlugin := true
  )

  val libSettings = Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-sbt" % "test-interface" % "1.0",
      compilerPlugin("org.scalamacros" % s"paradise" % "2.0.0" cross CrossVersion.full)
    ),
    name := "utest",
    unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".."/ "shared" / "main" / "scala"),
    unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".." / "shared" / "test" / "scala"),
    libraryDependencies ++= (
      if (scalaVersion.value startsWith "2.11.") Nil
      else Seq(
        "org.scalamacros" %% s"quasiquotes" % "2.0.0"
      )
    )
  )

  val sharedSettings = Seq(
    organization := "com.lihaoyi",
    scalaVersion := "2.10.4",
    version := "0.1.3",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    crossScalaVersions := Seq("2.10.4", "2.11.0"),
    // Sonatype
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
