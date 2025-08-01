package utest.framework

/**
 * Platform specific stuff that differs between JVM and JS
 */
trait PlatformShimsVersionSpecific {
  type EnableReflectiveInstantiation =
    org.portablescala.reflect.annotation.EnableReflectiveInstantiation

  val Reflect = org.portablescala.reflect.Reflect
}
