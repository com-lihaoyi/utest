package utest.asserts

import scala.reflect.api._
import scala.reflect.macros.Context

class MacroCompat(c: Context) {
  import c.universe._
  implicit class XtensionTree(tree: Universe#Tree) {
    def pos_=(pos: Universe#Position): Unit = {
      internal.setPos(tree.asInstanceOf[Tree], pos.asInstanceOf[Position])
    }
  }
}
