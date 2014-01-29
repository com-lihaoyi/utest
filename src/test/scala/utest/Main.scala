package utest

import scala.concurrent.ExecutionContext.Implicits.global
import utest.framework.{Result, TestSuite}
import scala.concurrent.ExecutionContext
import scala.util.Success
import utest.asserts.LoggedAssertionError

object Main {
  def main(args: Array[String]): Unit = {

    println("Core Tests")
    println(PlainFormatter.format(Core.tests.run()))
    println("Nesting Tests")
    println(PlainFormatter.format(Nesting.tests.run()))
    println("Parallel Tests")
    println(PlainFormatter.format(Parallel.tests.run()))
  }
}

