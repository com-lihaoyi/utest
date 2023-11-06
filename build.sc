import mill._, scalalib._, scalajslib._, scalanativelib._, publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`com.github.lolgab::mill-mima::0.0.24`
import com.github.lolgab.mill.mima._

val communityBuildDottyVersion = sys.props.get("dottyVersion").toList

val scalaVersions = "2.12.17" :: "2.13.10" :: "3.3.1" :: communityBuildDottyVersion

val scalaJSVersions = scalaVersions.map((_, "1.12.0"))
val scalaNativeVersions = scalaVersions.map((_, "0.4.16"))

val scalaReflectVersion = "1.1.2"

trait MimaCheck extends Mima {
  def mimaPreviousVersions = VcsVersion.vcsState().lastTag.toSeq
}

trait UtestModule extends PublishModule with MimaCheck with PlatformScalaModule{
  def artifactName = "utest"

  def crossScalaVersion: String

  // Temporary until the next version of Mima gets released with
  // https://github.com/lightbend/mima/issues/693 included in the release.
  def mimaPreviousArtifacts =
    if(crossScalaVersion.startsWith("3.")) Agg.empty[Dep] else super.mimaPreviousArtifacts()

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


trait UtestTestModule extends ScalaModule with TestModule {
  def testFramework = "test.utest.CustomFramework"
}

object utest extends Module {
  object jvm extends Cross[JvmUtestModule](scalaVersions)
  trait JvmUtestModule
    extends UtestMainModule with ScalaModule {
    def ivyDeps = Agg(
      ivy"org.scala-sbt:test-interface::1.0"
    ) ++ (if (crossScalaVersion.startsWith("2")) Agg(
      ivy"org.portable-scala::portable-scala-reflect::$scalaReflectVersion",
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    ) else Agg())
    object test extends ScalaTests with UtestTestModule{
      val crossScalaVersion = JvmUtestModule.this.crossScalaVersion
    }
  }

  object js extends Cross[JsUtestModule](scalaJSVersions)
  trait JsUtestModule extends UtestMainModule with ScalaJSModule with Cross.Module2[String, String]{
    def crossJSVersion = crossValue2
    def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-test-interface:$crossJSVersion".withDottyCompat(crossScalaVersion),
      ivy"org.portable-scala::portable-scala-reflect::$scalaReflectVersion".withDottyCompat(crossScalaVersion)
    ) ++ (if(crossScalaVersion.startsWith("2")) Agg(
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    ) else Agg())
    def scalaJSVersion = crossJSVersion
    object test extends ScalaJSTests with UtestTestModule{
      def offset = os.up
      val crossScalaVersion = JsUtestModule.this.crossScalaVersion
    }
  }

  object native extends Cross[NativeUtestModule](scalaNativeVersions)
  trait NativeUtestModule extends UtestMainModule with ScalaNativeModule with Cross.Module2[String, String]{
    def crossScalaNativeVersion = crossValue2
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scala-native::test-interface::$crossScalaNativeVersion"
    )

    def scalaNativeVersion = crossScalaNativeVersion
    object test extends ScalaNativeTests with UtestTestModule{
      val crossScalaVersion = NativeUtestModule.this.crossScalaVersion
    }
  }
}
