package microtest.runner

import sbt.testing.SubclassFingerprint
import microtest.DefaultFormatter


class Framework extends sbt.testing.Framework{
  def name(): String = "MicroTest"

  def fingerprints(): Array[sbt.testing.Fingerprint] = Array(
    new SubclassFingerprint {
      def superclassName = "microtest.framework.TestSuite"
      def isModule = true
      def requireNoArgConstructor = true
    }
  )

  def printer(args: Array[String]) = {
    def find[T](prefix: String, parse: String => T, default: T): T = {
      args.find(_.startsWith(prefix))
          .fold(default)(s => parse(s.drop(prefix.length)))
    }

    val color = find("--color=", _.toBoolean, true)
    val truncate = find("--truncate=", _.toInt, 30)
    val trace = find("--trace=", _.toBoolean, false)

    new DefaultFormatter(
      color,
      truncate,
      trace
    )
  }

  def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader): Runner = {
    new Runner(args, remoteArgs, printer(args))

  }
}
