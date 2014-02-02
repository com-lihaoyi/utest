import org.mozilla.javascript.{NativeObject, RhinoException}
import sbt.testing.{Logger, EventHandler, TaskDef}
import sbt.testing
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.scalajs.sbtplugin.environment.rhino.{Utilities, CodeBlock}
import scala.scalajs.sbtplugin.ScalaJSEnvironment

class Task(val taskDef: TaskDef,
           args: Array[String],
           path: String,
           environment: ScalaJSEnvironment,
           addCount: (Int, Int) => Unit,
           addResult: String => Unit)
           extends sbt.testing.Task{

  def tags(): Array[String] = Array()

  def execute(eventHandler: EventHandler, loggers: Array[Logger]): Array[testing.Task] = {

    def doStuff(s: Seq[String]) = {
      environment.runInContextAndScope { (context, scope) =>
        new CodeBlock(context, scope) with Utilities {
          try {
            val module = getModule(taskDef.fullyQualifiedName().replace('.', '_'))
            val results = callMethod(
              module,
              "runSuite",
              toScalaJSArray(args)
            )

            val Array(success, total, msg) = results.toString.split("\t", 3)
            addCount(success.toInt, total.toInt)

            addResult(msg)
          } catch {
            case t: RhinoException =>
              println(t.details, t.getScriptStack())
          }
        }
      }
    }

    val fqName = taskDef.fullyQualifiedName()

    if (fqName.startsWith(path)){
      doStuff(Nil)
    } else if (path.startsWith(fqName)){
      doStuff(path.drop(fqName.length).split("\\.").filter(_.length > 0))
    }else{
      // do nothing
    }

    Array()
  }
}
