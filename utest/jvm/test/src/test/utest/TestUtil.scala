package utest

object TestUtil {

  lazy val isDotty = {
    val cl: ClassLoader = Thread.currentThread().getContextClassLoader
    try {
      cl.loadClass("dotty.DottyPredef")
      true
    } catch {
      case _: ClassNotFoundException =>
        false
    }
  }

}
