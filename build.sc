import mill._, scalalib._, scalajslib._, scalanativelib._, publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.2.0`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`com.github.lolgab::mill-mima::0.0.12`
import com.github.lolgab.mill.mima._
import $ivy.`com.github.lolgab::mill-crossplatform::0.0.3`
import com.github.lolgab.mill.crossplatform._
import mill.scalalib.api.Util.isScala3

val communityBuildDottyVersion = sys.props.get("dottyVersion").toList

val scalaVersions = "2.11.12" :: "2.12.16" :: "2.13.8" :: "3.1.3" :: communityBuildDottyVersion

val scalaJSVersions = Seq("1.10.1")
val scalaNativeVersions = Seq("0.4.7")

val scalaReflectVersion = "1.1.2"

trait MimaCheck extends Mima {
  def mimaPreviousVersions = VcsVersion.vcsState().lastTag.toSeq
}

trait UtestModule extends PublishModule with MimaCheck {
  def artifactName = "utest"

  def crossScalaVersion: String

  // Temporary until the next version of Mima gets released with
  // https://github.com/lightbend/mima/issues/693 included in the release.
  def mimaPreviousArtifacts =
    if(isScala3(crossScalaVersion)) Agg.empty[Dep] else super.mimaPreviousArtifacts()

  def publishVersion = VcsVersion.vcsState().format()

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/com-lihaoyi/utest",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github(owner = "com-lihaoyi", repo = "utest"),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi")
    )
  )
}

trait UtestTestModule extends TestModule {
  def testFramework = "test.utest.CustomFramework"
}

object utest extends Cross[UtestCrossPlatformModule](scalaVersions: _*)
class UtestCrossPlatformModule(val crossScalaVersion: String) extends CrossPlatform {
  trait Shared extends CrossPlatformCrossScalaModule
  object jvm extends Shared with ScalaModule with UtestModule {
    def ivyDeps = Agg(
      ivy"org.scala-sbt:test-interface::1.0"
    ) ++ (if (crossScalaVersion.startsWith("2")) Agg(
      ivy"org.portable-scala::portable-scala-reflect::$scalaReflectVersion",
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    ) else Agg())
    object test extends CrossPlatformSources with Tests with UtestTestModule
  }

  object js extends Cross[JsUtestModule](scalaJSVersions: _*)
  class JsUtestModule(val crossScalaJSVersion: String)
    extends Shared with UtestModule with CrossScalaJSModule {
    def millSourcePath = super.millSourcePath / os.up
    def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-test-interface:$crossScalaJSVersion".withDottyCompat(crossScalaVersion),
      ivy"org.portable-scala::portable-scala-reflect::$scalaReflectVersion".withDottyCompat(crossScalaVersion)
    ) ++ (if(crossScalaVersion.startsWith("2")) Agg(
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    ) else Agg())
    object test extends CrossPlatformSources with Tests with UtestTestModule
  }

  object native extends Cross[NativeUtestModule](scalaNativeVersions: _*)
  class NativeUtestModule(val crossScalaNativeVersion: String)
    extends Shared with CrossScalaNativeModule with UtestModule {
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scala-native::test-interface::$crossScalaNativeVersion"
    )
    object test extends CrossPlatformSources with Tests with UtestTestModule
  }
}
