import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._

def commonSettings(s: String) = Seq(
  crossScalaVersions := Seq("2.10.4", "2.11.0"),
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
  unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".."/ ".."/ "shared" / "main" / "scala"),
  unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".."/ ".."/ s / "src" / "main" / "scala"),
  unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".."/ ".."/ "shared" / "test" / "scala"),
  unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".."/ ".."/ s / "src" / "test" / "scala")
)


lazy val js = project.in(file("src"))
  .settings(scalaJSSettings ++ commonSettings("."):_*)
  .settings(
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.1.3-JS",
    (loadedTestFrameworks in Test) := {
      (loadedTestFrameworks in Test).value.updated(
        sbt.TestFramework(classOf[utest.runner.JsFramework].getName),
        new utest.runner.JsFramework(environment = (scalaJSEnvironment in Test).value)
      )
    }
  )

lazy val jvm = project.in(file("jvm"))
  .settings(commonSettings("js"):_*)
  .settings(
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.1.3",
    testFrameworks += new TestFramework("utest.runner.JvmFramework")
  )