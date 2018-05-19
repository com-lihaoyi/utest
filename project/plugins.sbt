
val scalaJSVersion = Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.22")

addSbtPlugin("com.dwijnand"      % "sbt-dynver"               % "2.0.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"                  % "1.1.1")
addSbtPlugin("org.scala-js"      % "sbt-scalajs"              % scalaJSVersion)
addSbtPlugin("org.scala-native"  % "sbt-scala-native"         % "0.3.6" exclude("org.scala-native", "sbt-crossproject"))

{
  if (scalaJSVersion == "1.0.0-M1")
    Nil
  else
    Seq(addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.3.0"))
}
