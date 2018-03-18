package utest
package runner

import utest.framework.DefaultFormatters

object Framework{

}

/**
  * `MillFramework` this default to `true` since Mill's test-interface
  * support isn't buggy and doesn't need utest to rely on `println`
  */
class MillFramework extends Framework{
  override def useSbtLoggers = true
}
class Framework extends sbt.testing.Framework with framework.Formatter {

  def name(): String = "utest"

  /**
    * Override to run code before tests start running. Useful for setting up
    * global databases, initializing caches, etc.
    */
  def setup() = ()

  /**
    * Override to run code after tests finish running. Useful for shutting
    * down daemon processes, closing network connections, etc.
    */
  def teardown() = ()

  /**
    * How many tests need to run before uTest starts printing out the test
    * results summary and test failure summary at the end of a test run. For
    * small sets of tests, these aren't necessary since all the output fits
    * nicely on one screen; only when the number of tests gets large and their
    * output gets noisy does it become valuable to show the clean summaries at
    * the end of the test run.
    */
  def showSummaryThreshold = 30

  /**
    * Whether to use SBT's test-logging infrastructure, or just println.
    *
    * Defaults to println because SBT's test logging doesn't seem to give us
    * anything that we want, and does annoying things like making a left-hand
    * gutter and buffering input by default
    */
  def useSbtLoggers = false

  def resultsHeader = DefaultFormatters.resultsHeader
  def failureHeader = DefaultFormatters.failureHeader


  def startHeader(path: String) = DefaultFormatters.renderBanner("Running Tests" + path)


  final def fingerprints(): Array[sbt.testing.Fingerprint] = Array(Fingerprint)

  final def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader) = {
    new MasterRunner(
      args,
      remoteArgs,
      testClassLoader,
      setup _,
      teardown _,
      showSummaryThreshold,
      startHeader,
      resultsHeader,
      failureHeader,
      useSbtLoggers,
      this
    )
  }

  final def slaveRunner(args: Array[String],
                  remoteArgs: Array[String],
                  testClassLoader: ClassLoader,
                  send: String => Unit) = {
    new ScalaJsSlaveRunner(
      args,
      remoteArgs,
      testClassLoader,
      send,
      setup _,
      teardown _,
      useSbtLoggers,
      this
    )
  }
}
