package utest

import acyclic.file
import scala.concurrent.{Future, ExecutionContext}
import scala.language.experimental.macros

import scala.scalajs.js.annotation.JSExportDescendentObjects

@JSExportDescendentObjects
abstract class TestSuite

object TestSuite {
  def apply(expr: Unit): framework.Tree[framework.Test] = macro framework.TreeBuilder.applyImpl
}


