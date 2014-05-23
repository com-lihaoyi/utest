addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.5.0-M3")

resolvers += Resolver.url("scala-js-snapshots",
  url("http://repo.scala-js.org/repo/snapshots/")
)(Resolver.ivyStylePatterns)

// Test-specific requirements; comment out during the first +publishLocal
addSbtPlugin("com.lihaoyi" % "utest-js-plugin" % "0.1.4")

