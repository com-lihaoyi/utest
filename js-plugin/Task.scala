import org.mozilla.javascript.{NativeObject, RhinoException}
import sbt.testing.{Logger, EventHandler, TaskDef}
import sbt.testing
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.scalajs.sbtplugin.environment.rhino.{Utilities, CodeBlock}
import scala.scalajs.sbtplugin.ScalaJSEnvironment

class doStuff(f: Boolean => Unit, g: String => Unit){
  def apply__O__O(x: String) = {
    val Array(win, msg) = x.split("\\t", 2)
    f(win.toBoolean)
    g(msg)
  }
}
class doStuff2(f: Int => Unit){
  def apply__O__O(x: String) = f(x.toInt)
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
              new doStuff(addCount, msg => loggers.foreach(_.info(progressString + taskDef.fullyQualifiedName() + "." + msg))),
              new doStuff2(addTotal)
            )

            val Array(success, total, msg) = results.toString.split("\t", 3)

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
