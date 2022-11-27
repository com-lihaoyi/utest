import mill._, scalalib._, scalajslib._, scalanativelib._, publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.3.0`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`com.github.lolgab::mill-mima::0.0.13`
import com.github.lolgab.mill.mima._
import mill.scalalib.api.Util.isScala3

val communityBuildDottyVersion = sys.props.get("dottyVersion").toList

val scalaVersions = "2.11.12" :: "2.12.17" :: "2.13.10" :: "3.1.3" :: communityBuildDottyVersion

val scalaJSVersions = scalaVersions.map((_, "1.12.0"))
val scalaNativeVersions = scalaVersions.map((_, "0.4.9"))

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
abstract class UtestMainModule(crossScalaVersion: String) extends CrossScalaModule {
  def millSourcePath = super.millSourcePath / offset

  def offset: os.RelPath = os.rel
  def sources = T.sources(
    super.sources()
      .flatMap(source =>
        Seq(
          PathRef(source.path / os.up / source.path.last),
          PathRef(source.path / os.up / os.up / source.path.last),
        )
      )
  )
  override def docJar =
    if (crossScalaVersion.startsWith("2")) super.docJar
    else T {
      val outDir = T.ctx().dest
      val javadocDir = outDir / 'javadoc
      os.makeDir.all(javadocDir)
      mill.api.Result.Success(mill.modules.Jvm.createJar(Agg(javadocDir))(outDir))
    }
}


trait UtestTestModule extends ScalaModule with TestModule {
  def crossScalaVersion: String
  def testFramework = "test.utest.CustomFramework"
  def offset: os.RelPath = os.rel
  def millSourcePath = super.millSourcePath / os.up

  def sources = T.sources(
    super.sources()
      .++(CrossModuleBase.scalaVersionPaths(crossScalaVersion, s => millSourcePath / s"src-$s" ))
      .flatMap(source =>
        Seq(
          PathRef(source.path / os.up / "test" / source.path.last),
          PathRef(source.path / os.up / os.up / "test" / source.path.last),
        )
      )
      .distinct
  )
}

object utest extends Module {
  object jvm extends Cross[JvmUtestModule](scalaVersions: _*)
  class JvmUtestModule(val crossScalaVersion: String)
    extends UtestMainModule(crossScalaVersion) with ScalaModule with UtestModule {
    def ivyDeps = Agg(
      ivy"org.scala-sbt:test-interface::1.0"
    ) ++ (if (crossScalaVersion.startsWith("2")) Agg(
      ivy"org.portable-scala::portable-scala-reflect::$scalaReflectVersion",
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    ) else Agg())
    object test extends Tests with UtestTestModule{
      val crossScalaVersion = JvmUtestModule.this.crossScalaVersion
    }
  }

  object js extends Cross[JsUtestModule](scalaJSVersions: _*)
  class JsUtestModule(val crossScalaVersion: String, crossJSVersion: String)
    extends UtestMainModule(crossScalaVersion) with ScalaJSModule with UtestModule {
    def offset = os.up
    def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-test-interface:$crossJSVersion".withDottyCompat(crossScalaVersion),
      ivy"org.portable-scala::portable-scala-reflect::$scalaReflectVersion".withDottyCompat(crossScalaVersion)
    ) ++ (if(crossScalaVersion.startsWith("2")) Agg(
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    ) else Agg())
    def scalaJSVersion = crossJSVersion
    object test extends Tests with UtestTestModule{
      def offset = os.up
      val crossScalaVersion = JsUtestModule.this.crossScalaVersion
    }
  }

  object native extends Cross[NativeUtestModule](scalaNativeVersions: _*)
  class NativeUtestModule(val crossScalaVersion: String, crossScalaNativeVersion: String)
    extends UtestMainModule(crossScalaVersion) with ScalaNativeModule with UtestModule {
    def offset = os.up
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scala-native::test-interface::$crossScalaNativeVersion"
    )

    def scalaNativeVersion = crossScalaNativeVersion
    object test extends Tests with UtestTestModule{
      def offset = os.up
      val crossScalaVersion = NativeUtestModule.this.crossScalaVersion
    }
  }
}
