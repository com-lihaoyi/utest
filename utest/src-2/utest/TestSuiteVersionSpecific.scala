package utest

// import scala.reflect.macros.Context
import scala.language.experimental.macros

trait TestSuiteVersionSpecific {
  @deprecated("Use `utest.Tests{...}` instead")
  def apply(expr: Unit): Tests = macro TestsVersionSpecific.Builder.applyImpl
}

trait TestSuiteCompanionVersionSpecific {
  @deprecated("Use `utest.Tests{...}` instead")
  def apply(expr: Unit): Tests = macro TestsVersionSpecific.Builder.applyImpl
}


