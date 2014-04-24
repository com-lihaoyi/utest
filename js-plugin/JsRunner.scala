package utest.jsrunner
import sbt.testing._
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.annotation.tailrec
import scala.scalajs.sbtplugin.environment.rhino.{Utilities, CodeBlock}
import scala.scalajs.sbtplugin.ScalaJSEnvironment
import org.mozilla.javascript.{NativeObject, RhinoException}
import sbt.TestsFailedException
import utest.runner._
/**
 * Wraps a Scala callback in a Java-ish object that has the right signature
 * for Rhino to call.
 */
case class JsCallback(f: String => Unit){
  def apply__O__O(x: String) = f(x)
}

class JsRunner(val args: Array[String],
               val remoteArgs: Array[String],
               environment: ScalaJSEnvironment)
               extends GenericRunner{

  def doStuff(s: Seq[String], loggers: Seq[Logger], name: String) = {
    environment.runInContextAndScope { (context, scope) =>
      new CodeBlock(context, scope) with Utilities {
        try {
          val results = callMethod(
            getModule("utest_PlatformShims"),
            "runSuite",
            getModule(name.replace('.', '_')),
            toScalaJSArray(s.toArray),
            toScalaJSArray(args),
            JsCallback(s => if(s.toBoolean) success.incrementAndGet() else failure.incrementAndGet()),
            JsCallback(msg => loggers.foreach(_.info(progressString + name + "." + msg))),
            JsCallback(s => total.addAndGet(s.toInt))
          )
          addResult(results.toString)
        } catch {
          case t: RhinoException =>
            println(t.details, t.getScriptStack())
        }
      }
    }
  }
}

