import org.mozilla.javascript.{NativeObject, RhinoException}
import sbt.testing.{Logger, EventHandler, TaskDef}
import sbt.testing
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.scalajs.sbtplugin.environment.rhino.{Utilities, CodeBlock}
import scala.scalajs.sbtplugin.ScalaJSEnvironment

class Task(val taskDef: TaskDef,
           path: String,
           environment: ScalaJSEnvironment)
           extends sbt.testing.Task{

  println("Task " + path)
  def tags(): Array[String] = Array()

  def execute(eventHandler: EventHandler, loggers: Array[Logger]): Array[testing.Task] = {

    def doStuff(s: Seq[String]) = {
      println("Task.doStuff")
      println(s)
      println(taskDef.fullyQualifiedName())
      environment.runInContextAndScope { (context, scope) =>
        new CodeBlock(context, scope) with Utilities {
          try {
            val module = getModule(taskDef.fullyQualifiedName().replace('.', '_'))
            val called = callMethod(
              callMethod(module, "tests").asInstanceOf[NativeObject],
              "toString"
            )
            println("module " + module)
            println("called " + called)
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
