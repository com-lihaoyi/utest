package utest
package framework
//import acyclic.file

/**
 * An immutable tree with each node containing a value, and a `Seq` of
 * children. Provides all normal `Seq` functionality as well as some tree
 * specific methods.
 */
case class Tree[+T](value: T, children: Seq[Tree[T]]){
  /**
   * The number of nodes in this tree.
   */
  def length: Int = {
    children.foldLeft(1)(_ + _.length)
  }

  /**
   * An iterator over the values stored on the nodes of this tree, in a depth
   * first manner starting from the root.
   */
  def iterator: Iterator[T] = {
    Iterator(this.value) ++ children.flatMap(_.iterator)
  }

  def toSeq: Seq[T] = iterator.toList
  /**
   * Returns an iterator for the values at the leaves of this tree
   */
  def leaves: Iterator[T] = {
    if (children.isEmpty) Iterator(this.value)
    else children.toIterator.flatMap(_.leaves)
  }

  /**
   * Transforms this tree into a new tree by applying the function `f` to
   * the value at every node. Does not change the shape of the tree.
   */
  def map[V](f : T => V): Tree[V] = {
    Tree(f(value), children.map(_.map(f)))
  }

  /**
   * Transforms this tree into a new Tree by recursively walking the nodes
   * and applying the function `f` to each subtree to transform it. Starts from
   * the leaves and works its way back up to the root.
   */
  def scan[V](f: (T, Seq[Tree[V]]) => (V, Seq[Tree[V]])): Tree[V] = {
    (Tree.apply[V] _).tupled(f(value, children.map(_.scan(f))))
  }

  /**
   * Combines the values in this tree into a single value by recursively
   * collapsing each subtree using the function `f`, starting from the leaves
   * and working its way back up to the root.
   */
  def reduce[V >: T](f: (V, Seq[V]) => V): V = {
    f(value, children.map(_.reduce(f)))
  }
}
