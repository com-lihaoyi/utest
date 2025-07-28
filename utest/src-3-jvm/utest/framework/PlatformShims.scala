package utest.framework

/**
 * Platform specific stuff that differs between JVM and JS
 */
trait PlatformShimsVersionSpecific {
  type EnableReflectiveInstantiation =
    utest.framework.EnableReflectiveInstantiation

  val Reflect = PortableScalaReflectExcerpts
}
