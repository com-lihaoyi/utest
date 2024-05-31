import mill._, scalalib._, scalajslib._, scalanativelib._, publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`com.github.lolgab::mill-mima::0.1.1`
import com.github.lolgab.mill.mima._

val communityBuildDottyVersion = sys.props.get("dottyVersion").toList

val scalaVersions = "2.12.17" :: "2.13.10" :: "3.3.1" :: communityBuildDottyVersion

val scalaReflectVersion = "1.1.2"

trait MimaCheck extends Mima {
  def mimaPreviousVersions = VcsVersion.vcsState().lastTag.toSeq
}

trait UtestModule extends PublishModule with MimaCheck with PlatformScalaModule{
  def artifactName = "utest"

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
trait UtestMainModule extends CrossScalaModule with UtestModule


trait UtestTestModule extends TestModule {
  def testFramework = "test.utest.CustomFramework"
}

object utest extends Module {
  object jvm extends Cross[JvmUtestModule](scalaVersions)
  trait JvmUtestModule
    extends UtestMainModule {
    def ivyDeps = Agg(
      ivy"org.scala-sbt:test-interface::1.0"
    ) ++ (if (crossScalaVersion.startsWith("2")) Agg(
      ivy"org.portable-scala::portable-scala-reflect::$scalaReflectVersion",
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    ) else Agg())
    object test extends ScalaTests with UtestTestModule
  }

  object js extends Cross[JsUtestModule](scalaVersions)
  trait JsUtestModule extends UtestMainModule with ScalaJSModule{
    def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-test-interface:${scalaJSVersion()}".withDottyCompat(crossScalaVersion),
      ivy"org.portable-scala::portable-scala-reflect::$scalaReflectVersion".withDottyCompat(crossScalaVersion)
    ) ++ (if(crossScalaVersion.startsWith("2")) Agg(
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    ) else Agg())
    def scalaJSVersion = "1.12.0"
    object test extends ScalaJSTests with UtestTestModule
  }

  object native extends Cross[NativeUtestModule](scalaVersions)
  trait NativeUtestModule extends UtestMainModule with ScalaNativeModule {
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scala-native::test-interface::${scalaNativeVersion()}"
    )

    def scalaNativeVersion = "0.5.0"
    object test extends ScalaNativeTests with UtestTestModule
  }
}
