package utest.jsrunner
import sbt.testing.SubclassFingerprint

import utest.runner._
import scala.scalajs.tools.env.JSEnv
import scala.scalajs.tools.classpath.JSClasspath
import sbt._
import sbt.classpath.ClasspathFilter
import java.net.URLClassLoader

class JsFramework(environment: JSEnv) extends utest.runner.GenericTestFramework{
  def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader) = {
    val classpath = classLoader2Classpath(testClassLoader)

    val jsClasspath = JSClasspath.fromClasspath(classpath)
    println("::::::")
    println(classpath)
    println(jsClasspath.jsFiles.map(_.content).mkString)
    println(jsClasspath.jsDependencies)
    println(jsClasspath.mainJSFiles.map(_.content).mkString)
    new JsRunner(environment, jsClasspath, args, remoteArgs)
  }

  private def classLoader2Classpath(cl: ClassLoader): Seq[File] = cl match {
    case cl: URLClassLoader =>
      cl.getURLs().map(url => new File(url.toURI())).toList
    case sbtFilter: ClasspathFilter =>
      classLoader2Classpath(sbtFilter.getParent())
    case _ =>
      sys.error("You cannot use a Scala.js framework with a class loader of " +
        s"type: ${cl.getClass()}.")
  }
}
