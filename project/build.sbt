addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.0-M3")

/* uTest uses itself to test itself. At the library level, this is not a
 * problem. But the runner must be available to the build project. So we
 * simply add the sources of the runners to the build project.
 */
unmanagedSources in Compile ++= {
  val root = baseDirectory.value.getParentFile
  ((root / "runner" * "*.scala") +++ (root / "jsPlugin" * "*.scala")).get
}
