package utest

trait TestSuiteVersionSpecific {
  @deprecated("Use `utest.Tests{...}` instead")
  inline def apply(expr: => Unit): Tests = ${TestsVersionSpecific.testsImpl('expr)}
}

trait TestSuiteCompanionVersionSpecific {
  @deprecated("Use `utest.Tests{...}` instead")
  inline def apply(expr: => Unit): Tests = ${TestsVersionSpecific.testsImpl('expr)}
}
