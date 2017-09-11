package utest


import scala.annotation.meta._
import scala.util.{Failure, Success, Try}
import scala.collection.mutable.ArrayBuffer

/**
 * Macro powered `assert`s of all shapes and sizes. These asserts all use
 * macros to capture the names, types and values of variables used within
 * them, so you get nice error messages for free.
 */
package object asserts extends utest.asserts.Asserts
