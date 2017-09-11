
// 0.6.20 breaks Java7 - https://github.com/scala-js/scala-js/issues/3128
val scalaJSVersion = Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.19")

addSbtPlugin("com.jsuereth"      % "sbt-pgp"                  % "1.0.1")
addSbtPlugin("com.github.gseitz" % "sbt-release"              % "1.0.5")
addSbtPlugin("org.scala-js"      % "sbt-scalajs"              % scalaJSVersion)
addSbtPlugin("org.scala-native"  % "sbt-crossproject"         % "0.2.2")
addSbtPlugin("org.scala-native"  % "sbt-scala-native"         % "0.3.3")

{
  if (scalaJSVersion.startsWith("0.6."))
    Seq(addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.2.2"))
  else
    Nil
}
