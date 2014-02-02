import org.mozilla.javascript.{NativeObject, RhinoException}
import sbt.testing.{Logger, EventHandler, TaskDef}
import sbt.testing
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.scalajs.sbtplugin.environment.rhino.{Utilities, CodeBlock}
import scala.scalajs.sbtplugin.ScalaJSEnvironment


case class JsCallback(f: String => Unit){
  def apply__O__O(x: String) = f(x)
}

class Task(val taskDef: TaskDef,
           args: Array[String],
           path: String,
           environment: ScalaJSEnvironment,
           addCount: Boolean => Unit,
           addTotal: Int => Unit,
           addResult: String => Unit,
           progressString: => String)
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
              toScalaJSArray(args),
              JsCallback(s => addCount(s.toBoolean)),
              JsCallback(msg => loggers.foreach(_.info(progressString + taskDef.fullyQualifiedName() + "." + msg))),
              JsCallback(s => addTotal(s.toInt))
            )

            addResult(results.toString)
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
