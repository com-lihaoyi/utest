package utest.jsrunner
import sbt.testing._
import utest.runner._
import scala.scalajs.tools.env.{ConsoleJSConsole, JSEnv}
import scala.scalajs.tools.classpath._
import scala.scalajs.sbtplugin.testing.SbtTestLoggerAccWrapper
import scala.util.control.NonFatal
import scala.scalajs.sbtplugin.JSUtils._
import scala.scalajs.tools.io.MemVirtualJSFile

class JsRunner(environment: JSEnv,
               jsClasspath: CompleteClasspath,
               val args: Array[String],
               val remoteArgs: Array[String])
               extends GenericRunner{

  def doStuff(s: Seq[String], loggers: Seq[Logger], name: String): Unit = {

    val testRunnerFile =
      new MemVirtualJSFile("Generated test launcher file").withContent(s"""
        PlatformShims().runSuite(
          $name(),
          ${listToJS(s.toList)},
          ${listToJS(args.toList)}
        );
      """)

    val logger = new SbtTestLoggerAccWrapper(loggers)
    try {
      // Actually execute test
      environment.runJS(
        jsClasspath,
        testRunnerFile,
        logger,
        ConsoleJSConsole
      )
    } catch {
      case NonFatal(e) => logger.trace(e)
    }
  }
}

