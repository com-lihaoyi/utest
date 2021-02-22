package utest

trait TestSuiteVersionSpecific {
  @deprecated("Use `utest.Tests{...}` instead")
  transparent inline def apply(inline expr: Unit): Tests = ${TestsVersionSpecific.testsImpl('expr)}
}

trait TestSuiteCompanionVersionSpecific {
  @deprecated("Use `utest.Tests{...}` instead")
  transparent inline def apply(inline expr: Unit): Tests = ${TestsVersionSpecific.testsImpl('expr)}
}
