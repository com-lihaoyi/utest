package utest.jsrunner
import sbt.testing._
import utest.runner._
import scala.scalajs.tools.env.{JSConsole, ConsoleJSConsole, JSEnv}
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

  def doStuff(selector: Seq[String], loggers: Seq[Logger], name: String): Unit = {
    val testRunnerFile =
      new MemVirtualJSFile("Generated test launcher file").withContent(s"""
        PlatformShims().runSuite(
          $name(),
          ${listToJS(selector.toList)},
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
        new JSConsole {
          override def log(msg: Any): Unit = {
            msg.toString.split("/", 3) match{
              case Array("XXSecretXX", "addCount", s) => (if (s.toBoolean) success else failure).incrementAndGet()
              case Array("XXSecretXX", "log", s) => loggers.foreach(_.info(progressString + name + s))
              case Array("XXSecretXX", "addTotal", s) => total.addAndGet(s.toInt)
              case Array("XXSecretXX", "result", s) => addResult(s.replace("ZZZZ", "\n"))
              case _ => Console.println(msg)
            }
          }
        }
      )
    } catch {
      case NonFatal(e) => logger.trace(e)
    }
  }
}

