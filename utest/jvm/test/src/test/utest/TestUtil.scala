package utest

object TestUtil {

  lazy val isDotty = {
    val cl: ClassLoader = Thread.currentThread().getContextClassLoader
    try {
      cl.loadClass("scala.runtime.Scala3RunTime")
      true
    } catch {
      case _: ClassNotFoundException =>
        false
    }
  }

}
