package utest

import scala.concurrent.{Await, Future}
import concurrent.duration._

/**
 * Platform specific stuff that differs between JVM and JS
 */
trait PlatformShimsVersionSpecific {
  type EnableReflectiveInstantiation =
    utest.EnableReflectiveInstantiation

  val Reflect = PortableScalaReflectExcerpts
}
