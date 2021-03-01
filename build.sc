import mill._, scalalib._, scalajslib._, scalanativelib._, publish._

val dottyVersions = sys.props.get("dottyVersion").toList

val scala211 = "2.11.12"
val scala212 = "2.12.13"
val scala213 = "2.13.4"
val scala3 = "3.0.0-RC1"

val scalajs0 = "0.6.33"
val scalajs1 = "1.5.0"

val scalanative = "0.4.0"

val scalaVersions = scala211 :: scala212 :: scala213 :: scala3 :: dottyVersions

val scalaJSVersions = Seq(
  (scala211, scalajs0),
  (scala212, scalajs0),
  (scala213, scalajs0),
  (scala212, scalajs1),
  (scala213, scalajs1),
  (scala3, scalajs1)
) ++ dottyVersions.map(version => (version, scalajs1))

val scalaNativeVersions = Seq(
  (scala211, scalanative),
  (scala212, scalanative),
  (scala213, scalanative)
)

trait UtestModule extends PublishModule {
  def artifactName = "utest"

  def publishVersion = "0.7.7"

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/lihaoyi/utest",
    licenses = Seq(License.MIT),
    scm = SCM(
      "git://github.com/lihaoyi/utest.git",
      "scm:git://github.com/lihaoyi/utest.git"
    ),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi")
    )
  )
}
trait UtestMainModule extends CrossScalaModule {
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
}


trait UtestTestModule extends ScalaModule with TestModule {
  def crossScalaVersion: String
  def testFrameworks = Seq("test.utest.CustomFramework")
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
    extends UtestMainModule with ScalaModule with UtestModule {
    def ivyDeps = Agg(
      ivy"org.scala-sbt:test-interface::1.0"
    ) ++ (if (crossScalaVersion.startsWith("2")) Agg(
      ivy"org.portable-scala::portable-scala-reflect::0.1.1",
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    ) else Agg())
    object test extends Tests with UtestTestModule{
      val crossScalaVersion = JvmUtestModule.this.crossScalaVersion
    }

    override def docJar =
      if (crossScalaVersion.startsWith("2")) super.docJar
      else T {
        val outDir = T.ctx().dest
        val javadocDir = outDir / 'javadoc
        os.makeDir.all(javadocDir)
        mill.api.Result.Success(mill.modules.Jvm.createJar(Agg(javadocDir))(outDir))
      }
  }

  object js extends Cross[JsUtestModule](scalaJSVersions: _*)
  class JsUtestModule(val crossScalaVersion: String, crossJSVersion: String)
    extends UtestMainModule with ScalaJSModule with UtestModule {
    def offset = os.up
    def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-test-interface:$crossJSVersion",
      ivy"org.portable-scala::portable-scala-reflect::0.1.1",
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    )
    def scalaJSVersion = crossJSVersion
    object test extends Tests with UtestTestModule{
      def offset = os.up
      val crossScalaVersion = JsUtestModule.this.crossScalaVersion
    }
  }

  object native extends Cross[NativeUtestModule](scalaNativeVersions: _*)
  class NativeUtestModule(val crossScalaVersion: String, crossScalaNativeVersion: String)
    extends UtestMainModule with ScalaNativeModule with UtestModule {
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
