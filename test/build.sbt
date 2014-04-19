
lazy val js = project.in(file("."))
  .settings(
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.1.3-JS"
      (loadedTestFrameworks in Test) := {
      import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._
      (loadedTestFrameworks in Test).value.updated(
        sbt.TestFramework(classOf[utest.runner.JsFramework].getName),
        new utest.runner.JsFramework(environment = (scalaJSEnvironment in Test).value)
      )
    },
    unmanagedSourceDirectories in Compile <+= baseDirectory(_ / "shared" / "main" / "scala"),
    unmanagedSourceDirectories in Compile <+= baseDirectory(_ / "src" / "main" / "scala"),
    unmanagedSourceDirectories in Test <+= baseDirectory(_ / "shared" / "test" / "scala"),
    unmanagedSourceDirectories in Test <+= baseDirectory(_ / "src" / "test" / "scala")
  )

lazy val jvm = project.in(file("."))
  .settings(
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.1.3",
    testFrameworks += new TestFramework("utest.runner.JvmFramework"),
    unmanagedSourceDirectories in Compile <+= baseDirectory(_ / "shared" / "main" / "scala"),
    unmanagedSourceDirectories in Compile <+= baseDirectory(_ / "js" / "src" / "main" / "scala"),
    unmanagedSourceDirectories in Test <+= baseDirectory(_ / "shared" / "test" / "scala"),
    unmanagedSourceDirectories in Test <+= baseDirectory(_ / "js" / "src"/ "test" / "scala")
  )