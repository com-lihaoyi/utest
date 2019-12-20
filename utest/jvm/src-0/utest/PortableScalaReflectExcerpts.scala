package utest

import java.lang.reflect._
import scala.collection.mutable

/**
 * Copy-pasted from Portable Scala Reflect:
 * https://github.com/portable-scala/portable-scala-reflect/tree/e6d9e9f5b3dbbcd957c90ac501671b3311c2f485
 */

object PortableScalaReflectExcerpts {

  def lookupLoadableModuleClass(fqcn: String,
      loader: ClassLoader): Option[LoadableModuleClass] = {
    load(fqcn, loader).filter(isModuleClass).map(new LoadableModuleClass(_))
  }
  private def load(fqcn: String, loader: ClassLoader): Option[Class[_]] = {
    try {
      /* initialize = false, so that the constructor of a module class is not
       * executed right away. It will only be executed when we call
       * `loadModule`.
       */
      val clazz = Class.forName(fqcn, false, loader)
      if (inheritsAnnotation(clazz)) Some(clazz)
      else None
    } catch {
      case _: ClassNotFoundException => None
    }
  }

  private def isModuleClass(clazz: Class[_]): Boolean = {
    try {
      val fld = clazz.getField("MODULE$")
      clazz.getName.endsWith("$") && (fld.getModifiers & Modifier.STATIC) != 0
    } catch {
      case _: NoSuchFieldException => false
    }
  }

  private def inheritsAnnotation(clazz: Class[_]): Boolean = {
    val cache = mutable.Map.empty[Class[_], Boolean]

    def c(clazz: Class[_]): Boolean =
      cache.getOrElseUpdate(clazz, l(clazz))

    def l(clazz: Class[_]): Boolean = {
      if (clazz.getAnnotation(classOf[EnableReflectiveInstantiation]) != null) {
        true
      } else {
        (Iterator(clazz.getSuperclass) ++ clazz.getInterfaces.iterator)
          .filter(_ != null)
          .exists(c)
      }
    }

    c(clazz)
  }

  final class LoadableModuleClass private[PortableScalaReflectExcerpts] (val runtimeClass: Class[_]) {
    /** Loads the module instance and returns it.
     *
     *  If the underlying constructor throws an exception `e`, then `loadModule`
     *  throws `e`, unlike `java.lang.reflect.Field.get` which would wrap it in a
     *  `java.lang.reflect.ExceptionInInitializerError`.
     */
    def loadModule(): Any = {
      try {
        runtimeClass.getField("MODULE$").get(null)
      } catch {
        case e: java.lang.ExceptionInInitializerError =>
          val cause = e.getCause
          if (cause == null)
            throw e
          else
            throw cause
      }
    }
  }
}
