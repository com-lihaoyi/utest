package utest.framework

import java.lang.reflect.*
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

  private def isInstantiatableClass(clazz: Class[_]): Boolean = {
    /* A local class will have a non-null *enclosing* class, but a null
     * *declaring* class. For a top-level class, both are null, and for an
     * inner class (non-local), both are the same non-null class.
     */
    def isLocalClass: Boolean =
      clazz.getEnclosingClass() != clazz.getDeclaringClass()

    (clazz.getModifiers() & Modifier.ABSTRACT) == 0 &&
      clazz.getConstructors().length > 0 &&
      !isModuleClass(clazz) &&
      !isLocalClass
  }

  def lookupInstantiatableClass(fqcn: String,
                                loader: ClassLoader): Option[InstantiatableClass] = {
    load(fqcn, loader).filter(isInstantiatableClass).map(new InstantiatableClass(_))
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

  /** A wrapper for a class that can be instantiated.
   *
   * @param runtimeClass
   * The `java.lang.Class[_]` representing the class.
   */
  final class InstantiatableClass (val runtimeClass: Class[_]) {

    /** Instantiates this class using its zero-argument constructor.
     *
     * @throws java.lang.InstantiationException
     * (caused by a `NoSuchMethodException`)
     * If this class does not have a public zero-argument constructor.
     */
    def newInstance(): Any = {
      try {
        runtimeClass.newInstance()
      } catch {
        case e: IllegalAccessException =>
          /* The constructor exists but is private; make it look like it does not
           * exist at all.
           */
          throw new InstantiationException(runtimeClass.getName).initCause(
            new NoSuchMethodException(runtimeClass.getName + ".<init>()"))
      }
    }

  }
}
