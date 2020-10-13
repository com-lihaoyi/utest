package utest

trait TestSuiteVersionSpecific {
  @deprecated("Use `utest.Tests{...}` instead")
  inline def apply(inline expr: Unit): Tests = ${TestsVersionSpecific.testsImpl('expr)}
}

trait TestSuiteCompanionVersionSpecific {
  @deprecated("Use `utest.Tests{...}` instead")
  inline def apply(inline expr: Unit): Tests = ${TestsVersionSpecific.testsImpl('expr)}
}
