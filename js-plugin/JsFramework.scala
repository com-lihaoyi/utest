package utest.jsrunner


import utest.runner._
import scala.scalajs.tools.env.JSEnv
import scala.scalajs.sbtplugin.testing.JSClasspathLoader

class JsFramework(environment: JSEnv) extends utest.runner.GenericTestFramework{
  def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader) = {
    val jsClasspath = extractClasspath(testClassLoader)
    new JsRunner(environment, jsClasspath, args, remoteArgs)
  }


  /** extract classpath from ClassLoader (which must be a JSClasspathLoader) */
  private def extractClasspath(cl: ClassLoader) = cl match {
    case cl: JSClasspathLoader => cl.cp
    case _ =>
      sys.error("The Scala.js framework only works with a class loader of " +
        s"type JSClasspathLoader (${cl.getClass} given)")
  }
}
